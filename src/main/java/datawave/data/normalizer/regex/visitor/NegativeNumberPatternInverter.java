package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.AlternationNode;
import datawave.data.normalizer.regex.CharClassNode;
import datawave.data.normalizer.regex.CharRangeNode;
import datawave.data.normalizer.regex.EncodedPatternNode;
import datawave.data.normalizer.regex.ExpressionNode;
import datawave.data.normalizer.regex.GroupNode;
import datawave.data.normalizer.regex.Node;
import datawave.data.normalizer.regex.NodeListIterator;
import datawave.data.normalizer.regex.QuestionMarkNode;
import datawave.data.normalizer.regex.RegexConstants;
import datawave.data.normalizer.regex.RegexUtils;
import datawave.data.normalizer.regex.RepetitionNode;
import datawave.data.normalizer.regex.SingleCharNode;
import datawave.data.normalizer.regex.ZeroOrMoreNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static datawave.data.normalizer.regex.RegexUtils.toChar;
import static datawave.data.normalizer.regex.RegexUtils.toInt;

/**
 * Implementation of {@link CopyVisitor} that will return a copy of a regex tree with all patterns that are meant to match negative numbers inverted such that
 * they will match against negative numbers that were encoded by {@link datawave.data.type.util.NumericalEncoder}. The numerical encoder encodes negative
 * numbers such that the mantissa equals ten minus the mantissa of scientific notation.
 * 
 * @see datawave.data.type.util.NumericalEncoder
 */
public class NegativeNumberPatternInverter extends CopyVisitor {
    
    /**
     * Return a copy of the node tree with all negative patterns inverted.
     * 
     * @param node
     *            the node
     * @return the tree
     */
    public static Node invert(Node node) {
        if (node == null) {
            return null;
        }
        
        NegativeNumberPatternInverter visitor = new NegativeNumberPatternInverter();
        return (Node) node.accept(visitor, null);
    }
    
    @Override
    public Object visitEncodedPattern(EncodedPatternNode node, Object data) {
        // Operate on a copy of the pattern tree.
        Node copy = copy(node);
        
        // If the first character is not !, this is not a negative number pattern. Return the copy.
        if (!RegexUtils.isChar(copy.getFirstChild(), RegexConstants.EXCLAMATION_POINT)) {
            return copy;
        }
        
        // Create an initial encoded pattern node with all the leading bin info.
        EncodedPatternNode encodedPattern = new EncodedPatternNode();
        List<Node> children = copy.getChildren();
        int startOfNodesToInvert = 0;
        for (Node child : children) {
            startOfNodesToInvert++;
            encodedPattern.addChild(child);
            if (RegexUtils.isChar(child, RegexConstants.CAPITAL_E)) {
                break;
            }
        }
        
        // Invert the remaining nodes and add them to the encoded pattern node.
        List<Node> nodesToInvert = new ArrayList<>(children.subList(startOfNodesToInvert, children.size()));
        encodedPattern.addChildren(new Inverter(nodesToInvert).invert());
        return encodedPattern;
    }
    
    private static class Inverter {
        
        private static final int TEN = 10;
        private static final int NINE = 9;
        
        /**
         * Minuend: a quantity or number from which another is to be subtracted.
         */
        private enum Minuend {
            /**
             * Represents a minuend of 10.
             */
            TEN,
            /**
             * Represents a minuend of 9.
             */
            NINE,
            /**
             * Represents a minuend of 9 or 10. When an element is being inverted, and all other elements after in the pattern may occur zero times, the element
             * must be inverted such that both variants of a minuend of 9 or a minuend of 10 are represented.
             */
            NINE_OR_TEN
        }
        
        // The node iterator.
        private final NodeListIterator iter;
        
        // The currently inverted nodes.
        private final List<Node> inverted = new ArrayList<>();
        
        // The current minuend.
        private Minuend currentMinuend;
        
        // The most recent element.
        Node currentElement;
        
        // The most recent quantifier.
        Node currentQuantifier;
        
        // The most recent question mark.
        Node currentQuestionMark;
        
        // Whether the current element was expanded to a group after being inverted.
        boolean currentElementExpandedToGroup;
        
        Inverter(List<Node> nodesToInvert) {
            this(nodesToInvert, Minuend.TEN);
        }
        
        Inverter(List<Node> nodesToInvert, Minuend initialMinuend) {
            List<Node> nodes = new ArrayList<>(nodesToInvert);
            Collections.reverse(nodes);
            this.iter = new NodeListIterator(nodes);
            this.currentMinuend = initialMinuend;
        }
        
        /**
         * Return the list of nodes where each element has been inverted.
         * 
         * @return the inverted nodes
         */
        private List<Node> invert() {
            // Invert the last element of the pattern (first element of the iterator) using the initial minuend.
            invertNext();
            
            // Invert the remaining elements .
            while (iter.hasNext()) {
                invertNext();
            }
            
            // Elements have been added to the inverted list in reverse order. Correct the order before returning.
            Collections.reverse(inverted);
            return inverted;
        }
        
        /**
         * Invert the next element in the node list iterator.
         */
        private void invertNext() {
            // Capture the next element, along with its quantifier, and question mark, if present.
            captureNext();
            
            switch (currentElement.getType()) {
                case ESCAPED_SINGLE_CHAR:
                case ANY_CHAR:
                case DIGIT_CHAR_CLASS:
                    // If the current element is a decimal point, wildcard or digit character class, no inversion needs to happen. Add the current element nodes
                    // to the inverted list in reverse order.
                    addCurrentQuestionMarkToInverted();
                    addCurrentQuantifierToInverted();
                    addCurrentElementToInverted();
                    break;
                case GROUP:
                case CHAR_CLASS:
                case SINGLE_CHAR:
                    // Invert the current element.
                    Node invertedElement = invertCurrentElement();
                    if (currentElementExpandedToGroup) {
                        // If the current element was expanded to a group, only add the question mark to the inverted list before adding the group. If a
                        // quantifier
                        // was present, it will be in the group.
                        addCurrentQuestionMarkToInverted();
                        inverted.add(invertedElement);
                    } else {
                        // Otherwise add the question mark, quantifier, and inverted element to the inverted list in reverse order.
                        addCurrentQuestionMarkToInverted();
                        addCurrentQuantifierToInverted();
                        inverted.add(invertedElement);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Encountered unhandled element type: " + currentElement.getType());
            }
            
            // Determine if the minuend for the next element should be both nine and 10.
            if (nextMinuendShouldBeNineOrTen()) {
                currentMinuend = Minuend.NINE_OR_TEN;
            } else {
                // Otherwise, the minuend should be nine for subsequent elements.
                currentMinuend = Minuend.NINE;
            }
        }
        
        /**
         * Add the current question mark to the inverted list if it is not null.
         */
        private void addCurrentQuestionMarkToInverted() {
            if (currentQuestionMark != null) {
                inverted.add(currentQuestionMark);
            }
        }
        
        /**
         * Add the current quantifier to the inverted list if it is not null.
         */
        private void addCurrentQuantifierToInverted() {
            if (currentQuantifier != null) {
                inverted.add(currentQuantifier);
            }
        }
        
        /**
         * Add the current element to the inverted list.
         */
        private void addCurrentElementToInverted() {
            inverted.add(currentElement);
        }
        
        /**
         * Determine if the minuend for the next element should be {@link Minuend#NINE_OR_TEN}. The minuend should be {@link Minuend#NINE_OR_TEN} if and only if
         * thus far all previously inverted elements in the pattern could occur zero times, or match a zero character that would be subsequently trimmed when
         * encoded by {@link datawave.data.type.util.NumericalEncoder}.
         * 
         * @return true if the next minuend should be {@link Minuend#NINE_OR_TEN}, or false otherwise
         */
        private boolean nextMinuendShouldBeNineOrTen() {
            // The next minuend can be NINE_OR_TEN only if the current minuend is TEN or NINE_OR_TEN.
            if (currentMinuend == Minuend.TEN || currentMinuend == Minuend.NINE_OR_TEN) {
                // If the current element can occur zero times, either due to *, {0}, or {0,x}, the next minuend can be NINE_OR_TEN.
                if (currentQuantifier != null) {
                    if (currentQuantifier instanceof ZeroOrMoreNode) {
                        return true;
                    } else if (currentQuantifier instanceof RepetitionNode) {
                        return RegexUtils.canOccurZeroTimes((RepetitionNode) currentQuantifier);
                    }
                } else {
                    // If there is no quantifier, the minuend can NINE_OR_TEN if the current element is a question mark.
                    return currentQuestionMark != null;
                }
            }
            return false;
        }
        
        /**
         * Capture the next element, quantifier, and current question mark. Additionally, reset {@link #currentElementExpandedToGroup} to false.
         */
        private void captureNext() {
            // Reset current items to null.
            currentElement = null;
            currentQuantifier = null;
            currentQuestionMark = null;
            currentElementExpandedToGroup = false;
            
            // Extract the next element, quantifier, and
            while (iter.hasNext()) {
                Node prev = iter.next();
                if (prev instanceof QuestionMarkNode) {
                    // If the next element is a question mark, it does not need to be inverted. Add it to the list of inverted elements.
                    currentQuestionMark = prev;
                } else if (RegexUtils.isQuantifier(prev)) {
                    // If the next element is a quantifier, it does not need to be inverted. Add it to the list of inverted elements.
                    currentQuantifier = prev;
                } else {
                    // If we've reached an element that is not a question mark or quantifier, we've reached an element that may need to be inverted.
                    currentElement = prev;
                    break;
                }
            }
        }
        
        /**
         * Invert the current node based on its type and return the inversion.
         * 
         * @return the inverted node
         */
        private Node invertCurrentElement() {
            switch (currentElement.getType()) {
                case SINGLE_CHAR:
                    return invertCurrentSingleChar();
                case CHAR_CLASS:
                    return invertCurrentCharacterClass();
                case GROUP:
                    return invertCurrentGroup();
                default:
                    throw new IllegalArgumentException("Unhandled element type " + currentElement.getType());
            }
        }
        
        /**
         * The current element is a single character node. Invert the value.
         */
        private Node invertCurrentSingleChar() {
            SingleCharNode currentSingleChar = (SingleCharNode) currentElement;
            switch (currentMinuend) {
                case TEN:
                    // Subtract the value from 10.
                    return subtractFrom(TEN, currentSingleChar);
                case NINE:
                    // Subtract the value from 9.
                    return subtractFrom(NINE, currentSingleChar);
                case NINE_OR_TEN:
                    // If the current element does not have a quantifier, return a character class with the results of the value subtracted from 10 and 9.
                    if (currentQuantifier == null) {
                        CharClassNode charClass = new CharClassNode();
                        charClass.addChild(subtractFrom(TEN, currentSingleChar));
                        charClass.addChild(subtractFrom(NINE, currentSingleChar));
                        return charClass;
                    } else {
                        currentElementExpandedToGroup = true;
                        // The current element has a quantifier. We must create a group with the results separated by an alternation with their quantifier.
                        AlternationNode alternation = new AlternationNode();
                        ExpressionNode left = new ExpressionNode(subtractFrom(TEN, currentSingleChar));
                        left.addChild(copy(currentQuantifier));
                        alternation.addChild(left);
                        ExpressionNode right = new ExpressionNode(subtractFrom(NINE, currentSingleChar));
                        right.addChild(copy(currentQuantifier));
                        alternation.addChild(right);
                        return new GroupNode(alternation);
                    }
                default:
                    throw new IllegalArgumentException("Unsupported minuend " + currentMinuend);
            }
        }
        
        /**
         * The current element is a character class. Invert the character class.
         */
        private Node invertCurrentCharacterClass() {
            CharClassNode currentCharClass = (CharClassNode) currentElement;
            // Create a shallow copy in order to retain whether the char class is negated.
            switch (currentMinuend) {
                case TEN:
                    // Subtract all values from 10.
                    return subtractFrom(TEN, currentCharClass);
                case NINE:
                    // Subtract all values from 9.
                    return subtractFrom(NINE, currentCharClass);
                case NINE_OR_TEN:
                    currentElementExpandedToGroup = true;
                    // Subtract all values from 9 and 10, and store both char classes as alternations in a group.
                    AlternationNode alternation = new AlternationNode();
                    ExpressionNode left = new ExpressionNode(subtractFrom(TEN, currentCharClass));
                    // Add the current quantifier if not null.
                    if (currentQuantifier != null) {
                        left.addChild(copy(currentQuantifier));
                    }
                    alternation.addChild(left);
                    
                    // Add the current quantifier if not null.
                    ExpressionNode right = new ExpressionNode(subtractFrom(NINE, currentCharClass));
                    if (currentQuantifier != null) {
                        right.addChild(copy(currentQuantifier));
                    }
                    alternation.addChild(right);
                    return new GroupNode(alternation);
                default:
                    throw new IllegalArgumentException("Unsupported minuend " + currentMinuend);
            }
        }
        
        /**
         * Subtract the given single char node from the given minuend and return the result.
         */
        private SingleCharNode subtractFrom(int minuend, SingleCharNode node) {
            char digit = node.getCharacter();
            int value = minuend - toInt(digit);
            return new SingleCharNode(toChar(value));
        }
        
        /**
         * Subtract the given char class node from the given minuend and return the result.
         */
        private CharClassNode subtractFrom(int minuend, CharClassNode node) {
            // Make a shallow copy to also copy over whether the char class is negated.
            CharClassNode charClass = node.shallowCopy();
            // Subtract all values from 9.
            for (Node child : node.getChildren()) {
                if (child instanceof SingleCharNode) {
                    charClass.addChild(subtractFrom(minuend, (SingleCharNode) child));
                } else {
                    charClass.addChild(subtractFrom(minuend, (CharRangeNode) child));
                }
            }
            return charClass;
        }
        
        /**
         * Subtract the given range from the given minuend and return the result.
         */
        private CharRangeNode subtractFrom(int minuend, CharRangeNode node) {
            int startValue = minuend - toInt(node.getStart());
            int endValue = minuend - toInt(node.getEnd());
            // If the start value is equal to or less than the end value, return the range as (start-end). Otherwise, return the range as (end-start).
            if (startValue <= endValue) {
                return new CharRangeNode(toChar(startValue), toChar(endValue));
            } else {
                return new CharRangeNode(toChar(endValue), toChar(startValue));
            }
        }
        
        /**
         * The current element is a group. Invert the group.
         */
        public Node invertCurrentGroup() {
            // Invert the group's children. A group is only present here for consolidated leading/trailing zeros that need to be retained for a potential match.
            // In this case, the desired minuend is always 9.
            Inverter inverter = new Inverter(currentElement.getChildren(), Minuend.NINE);
            List<Node> children = inverter.invert();
            return new GroupNode(new ExpressionNode(children));
        }
    }
}
