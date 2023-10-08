package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.EncodedPatternNode;
import datawave.data.normalizer.regex.EscapedSingleCharNode;
import datawave.data.normalizer.regex.GroupNode;
import datawave.data.normalizer.regex.IntegerNode;
import datawave.data.normalizer.regex.IntegerRangeNode;
import datawave.data.normalizer.regex.Node;
import datawave.data.normalizer.regex.NodeListIterator;
import datawave.data.normalizer.regex.NodeType;
import datawave.data.normalizer.regex.OneOrMoreNode;
import datawave.data.normalizer.regex.QuestionMarkNode;
import datawave.data.normalizer.regex.RegexConstants;
import datawave.data.normalizer.regex.RegexUtils;
import datawave.data.normalizer.regex.RepetitionNode;
import datawave.data.normalizer.regex.ZeroOrMoreNode;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of {@link CopyVisitor} that return a copy of a regex tree with decimal places inserted where required in encoded regex patterns. Patterns
 * starting with an element that has a quantifier {@code (* + or {x})} will see the quantifier modified as required to ensure a decimal place is inserted
 * correctly. Multiple optional decimal points may be added to a single regex pattern.
 */
public class DecimalPointPlacer extends CopyVisitor {
    
    public static Node addDecimalPoints(Node node) {
        if (node == null) {
            return null;
        }
        DecimalPointPlacer visitor = new DecimalPointPlacer();
        return (Node) node.accept(visitor, null);
    }
    
    @Override
    public Object visitEncodedPattern(EncodedPatternNode node, Object data) {
        // Operate on a copy of the node.
        Node copy = copy(node);
        
        // Create an initial encoded pattern node with all the leading bin info.
        EncodedPatternNode encodedPattern = new EncodedPatternNode();
        NodeListIterator iter = copy.getChildrenIterator();
        while (iter.hasNext()) {
            Node next = iter.next();
            encodedPattern.addChild(next);
            if (RegexUtils.isChar(next, RegexConstants.CAPITAL_E)) {
                break;
            }
        }
        
        // Get a list of nodes with decimal points added and add them to the pattern node.
        DecimalPointAdder adder = new DecimalPointAdder(iter);
        List<Node> nodes = adder.addDecimalPoints();
        encodedPattern.addChildren(nodes);
        
        // Add the remaining children to the pattern node.
        while (iter.hasNext()) {
            encodedPattern.addChild(iter.next());
        }
        return encodedPattern;
    }
    
    private static class DecimalPointAdder {
        
        // The node iterator.
        private final NodeListIterator iter;
        
        // The nodes enriched with decimal points.
        private final List<Node> nodes = new ArrayList<>();
        
        // The most recent element.
        private Node currentElement;
        
        // The most recent quantifier.
        private Node currentQuantifier;
        
        // The most recent question mark.
        private Node currentQuestionMark;
        
        // Whether any decimal points have been added.
        boolean addedAnyDecimalPoints = false;
        
        // Whether additional optional decimal points should be added.
        boolean addMoreDecimalPoints = true;
        
        // Whether a non-leading zero has been seen.
        boolean nonLeadingZeroSeen = false;
        
        public DecimalPointAdder(NodeListIterator iter) {
            this.iter = iter;
        }
        
        /**
         * Return a list of nodes enriched with decimal points. This list is not guaranteed to contain all nodes found within the iterator supplied to
         * {@link #DecimalPointAdder(NodeListIterator)}, so subsequent calls to {@link NodeListIterator#next()} should be made to the iterator after the fact to
         * retrieve any remaining nodes.
         */
        public List<Node> addDecimalPoints() {
            // If we can skip adding decimal points, do so.
            if (skipAddingDecimalPoints()) {
                return nodes;
            }
            
            // Add decimal points until either there are no more elements or if we have created a final decimal point.
            while (iter.hasNext() && addMoreDecimalPoints) {
                // Capture the current element, quantifier, and optional.
                captureNext();
                
                switch (currentElement.getType()) {
                    case GROUP:
                        addGroup();
                        break;
                    case ANY_CHAR:
                    case CHAR_CLASS:
                    case DIGIT_CHAR_CLASS:
                    case SINGLE_CHAR:
                        // If we have seen a non-leading zero, mark it so.
                        if (!RegexUtils.matchesZero(currentElement)) {
                            nonLeadingZeroSeen = true;
                        }
                        // Quantified characters must be handled differently from non-quantified characters.
                        if (currentQuantifier == null) {
                            addNonQuantifiedElement();
                        } else {
                            addQuantifiedElement();
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled element type: " + currentElement.getType());
                }
                
                // Mark whether we've added any decimal points only after processing the first decimal point.
                addedAnyDecimalPoints = true;
            }
            
            return nodes;
        }
        
        /**
         * Return whether the entire pattern after the bin information consists of .*, .+, or a non-quantified element.
         * 
         * @return true if decimal points do not need to be added to this pattern, or false otherwise
         */
        private boolean skipAddingDecimalPoints() {
            int originalIndex = iter.index();
            boolean skipAddingDecimalPoints = false;
            
            while (iter.hasNext()) {
                Node element = iter.next();
                Node quantifier = iter.isNextQuantifier() ? iter.next() : null;
                iter.seekPastQuestionMarks();
                
                // If there is a second element, we cannot skip adding decimal points.
                if (iter.hasNext()) {
                    break;
                } else {
                    // If the sole element is a wildcard, we do not need to add decimal points if it is '.' '.*' or '.+'.
                    if (element.getType() == NodeType.ANY_CHAR) {
                        skipAddingDecimalPoints = quantifier == null || quantifier instanceof ZeroOrMoreNode || quantifier instanceof OneOrMoreNode;
                    } else {
                        // If sole element is not a wildcard, but has no quantifier, we do not need to add decimal points.
                        skipAddingDecimalPoints = quantifier == null;
                    }
                }
            }
            
            // Reset the iterator back to the original index.
            iter.setIndex(originalIndex);
            return skipAddingDecimalPoints;
        }
        
        /**
         * Capture the next element, quantifier, and optional.
         */
        private void captureNext() {
            currentElement = iter.next();
            currentQuantifier = iter.isNextQuantifier() ? iter.next() : null;
            currentQuestionMark = iter.isNextQuestionMark() ? iter.next() : null;
        }
        
        /**
         * The current element is an optional group of leading zeros with a defined range that must occur more than once.
         */
        private void addGroup() {
            Node innerElement = currentElement.getFirstChild();
            Node innerQuantifier = currentElement.getChildAt(1);
            Node innerQuestionMark = currentElement.getChildCount() == 3 ? currentElement.getChildAt(2) : null;
            
            // If the inner element can only match zero, we do not need to insert any decimal points. Add them as is.
            if (RegexUtils.matchesZeroOnly(innerElement)) {
                addAllCurrentToNodes();
            } else {
                // Get the group's children with a decimal point inserted where appropriate. Require the decimal point to be optional.
                List<Node> nodes = getRepetitionQuantifiedElements(innerElement, innerQuantifier, innerQuestionMark, true);
                GroupNode groupNode = new GroupNode();
                groupNode.addChildren(nodes);
                this.nodes.add(groupNode);
                this.nodes.add(new QuestionMarkNode());
            }
        }
        
        /**
         * Add a decimal point based on a current element that is not quantified.
         */
        private void addNonQuantifiedElement() {
            // Add the current nodes.
            addCurrentElementToNodes();
            addCurrentQuestionMarkToNodes();
            
            // If this is the last element in the regex expression, do not add any decimal points.
            if (!iter.hasNext()) {
                return;
            }
            
            // Add a decimal point.
            addDecimalPointToNodes();
            
            if (currentQuestionMark != null) {
                // If the current element is optional, make the decimal point optional.
                addQuestionMarkToNodes();
            } else {
                // Otherwise if we have added any optional decimal points before this one, or the remaining pattern can be zero-length, make the decimal point
                // optional.
                if (addedAnyDecimalPoints || remainingPatternCanBeZeroLength()) {
                    addQuestionMarkToNodes();
                }
                // Stop adding more decimal points.
                addMoreDecimalPoints = false;
            }
        }
        
        /**
         * Add decimal points based on a current element that is quantified.
         */
        private void addQuantifiedElement() {
            switch (currentQuantifier.getType()) {
                case ZERO_OR_MORE:
                    // Add decimal point for quantifier *.
                    addZeroOrMoreQuantifiedElement();
                    break;
                case ONE_OR_MORE:
                    // Add decimal point for quantifier +.
                    addOneOrMoreQuantifiedElement();
                    break;
                case REPETITION:
                    // Add decimal point for quantifier {x}.
                    this.nodes.addAll(getRepetitionQuantifiedElements(currentElement, currentQuantifier, currentQuestionMark, false));
                    break;
            }
        }
        
        /**
         * Add a decimal point for a current element that is followed by *.
         */
        private void addZeroOrMoreQuantifiedElement() {
            // If the current element is a wildcard, we're looking at .* and can add it as is.
            if (currentElement.getType() == NodeType.ANY_CHAR) {
                addAllCurrentToNodes();
            } else {
                // Add an optional variant of the current element.
                addCurrentElementToNodes();
                addQuestionMarkToNodes();
                // Add an optional decimal point.
                addDecimalPointToNodes();
                addQuestionMarkToNodes();
                // Add the current element again, followed by the current quantifier and optional.
                addAllCurrentToNodes();
            }
        }
        
        /**
         * Add a decimal point for a current element that is followed by +.
         */
        private void addOneOrMoreQuantifiedElement() {
            // Add the current element, non-optional.
            addCurrentElementToNodes();
            // Add an optional decimal point.
            addDecimalPointToNodes();
            addQuestionMarkToNodes();
            // Add the current element again, but this time followed by a *, as well as the current optional.
            addCurrentElementToNodes();
            nodes.add(new ZeroOrMoreNode());
            addCurrentQuestionMarkToNodes();
            // Do not add any more decimal points after this.
            addMoreDecimalPoints = false;
        }
        
        /**
         * Add decimal points for a current element that is followed by a repetition.
         */
        private List<Node> getRepetitionQuantifiedElements(Node element, Node quantifier, Node questionMark, boolean makeDecimalOptional) {
            List<Node> nodes = new ArrayList<>();
            
            // Add an initial copy of the current element.
            nodes.add(copy(element));
            
            // Get the repetition range from the quantifier node.
            Pair<Integer,Integer> repetitionRange = getRepetitionRange((RepetitionNode) quantifier);
            boolean elementMarkedOptional = false;
            if (repetitionRange.getLeft() == 0) {
                // If the repetition range starts with 0, either {0,} or {0,x}, make the first occurrence of the element optional.
                nodes.add(new QuestionMarkNode());
                elementMarkedOptional = true;
            }
            
            // Subtract one from both endpoints of the repetition since we have added an initial single copy of the element to the nodes already. What we do
            // next will depend on what the updated repetition range now covers.
            repetitionRange = subtractOneFrom(repetitionRange);
            
            // The new repetition range is {0,}, which is equivalent to *.
            if (repetitionRange.getLeft() == 0 && repetitionRange.getRight() == null) {
                nodes.add(createDecimalPoint());
                nodes.add(new QuestionMarkNode());
                nodes.add(copy(element));
                nodes.add(new ZeroOrMoreNode());
                if (questionMark != null) {
                    nodes.add(copy(questionMark));
                }
            } else if (repetitionRange.getLeft() == 1 && repetitionRange.getRight() == null) {
                // The new repetition range is {1,}, which is equivalent to +.
                nodes.add(createDecimalPoint());
                if (makeDecimalOptional) {
                    nodes.add(new QuestionMarkNode());
                }
                nodes.add(copy(element));
                nodes.add(new OneOrMoreNode());
                if (questionMark != null) {
                    nodes.add(copy(questionMark));
                }
            } else if (repetitionRange.getRight() == null) {
                // The new repetition range is {x,}.
                nodes.add(createDecimalPoint());
                if (makeDecimalOptional) {
                    nodes.add(new QuestionMarkNode());
                }
                nodes.add(copy(element));
                nodes.add(createRepetition(repetitionRange));
                if (questionMark != null) {
                    nodes.add(copy(questionMark));
                }
            } else if (repetitionRange.getLeft() == 0 && repetitionRange.getRight() > 0) {
                // The new repetition range is {0,x}.
                nodes.add(createDecimalPoint());
                // If either we're looking at an optional group, or we have added any decimal points before, or we have not seen a non-leading zero,
                // or there is only one more element, or the remaining pattern can be zero-length, make the decimal point optional.
                if (iter.hasNext()) {
                    if (makeDecimalOptional || addedAnyDecimalPoints || !nonLeadingZeroSeen || remainingPatternCanBeZeroLength()
                                    || remainingPatternHasOnlyOneMoreElement()) {
                        nodes.add(new QuestionMarkNode());
                    }
                } else {
                    nodes.add(new QuestionMarkNode());
                }
                nodes.add(copy(element));
                nodes.add(createRepetition(repetitionRange));
                if (questionMark != null) {
                    nodes.add(copy(questionMark));
                }
            } else if (repetitionRange.getLeft() == 1 && repetitionRange.getRight() == 1) {
                // The new repetition range is {1,1}. Another instance of the element can be added without a repetition after it.
                nodes.add(createDecimalPoint());
                if (makeDecimalOptional) {
                    nodes.add(new QuestionMarkNode());
                }
                nodes.add(copy(element));
            } else if (repetitionRange.getLeft() > 0 || repetitionRange.getRight() > 0) {
                // The new repetition range is {x,y}. Add an instance of the element with the repetition after it.
                nodes.add(createDecimalPoint());
                if (makeDecimalOptional) {
                    nodes.add(new QuestionMarkNode());
                }
                nodes.add(copy(element));
                nodes.add(createRepetition(repetitionRange));
                if (questionMark != null) {
                    nodes.add(copy(questionMark));
                }
            } else if (repetitionRange.getLeft() == 0 && repetitionRange.getRight() == 0) {
                // The new repetition range is {0,0}. Do not add another instance of the element. If the remaining pattern cam be zero-length, or the first
                // instance of the element was marked optional, make the decimal point optional.
                if (iter.hasNext()) {
                    nodes.add(createDecimalPoint());
                    if (makeDecimalOptional || remainingPatternCanBeZeroLength() || elementMarkedOptional) {
                        nodes.add(new QuestionMarkNode());
                    }
                }
            }
            if (nonLeadingZeroSeen) {
                addMoreDecimalPoints = false;
            }
            return nodes;
        }
        
        /**
         * Add a copy of {@link #currentElement} to the node list.
         */
        private void addCurrentElementToNodes() {
            nodes.add(copy(currentElement));
        }
        
        /**
         * Add a copy of {@link #currentQuantifier} to the node list if it is not null.
         */
        private void addCurrentQuantifierToNodes() {
            if (currentQuantifier != null) {
                nodes.add(copy(currentQuantifier));
            }
        }
        
        /**
         * Add a copy of {@link #currentQuestionMark} to the node list if it is not null.
         */
        private void addCurrentQuestionMarkToNodes() {
            if (currentQuestionMark != null) {
                nodes.add(copy(currentQuestionMark));
            }
        }
        
        /**
         * Add the current element, quantifier, and question mark to the node list.
         */
        private void addAllCurrentToNodes() {
            addCurrentElementToNodes();
            addCurrentQuantifierToNodes();
            addCurrentQuestionMarkToNodes();
        }
        
        /**
         * Add a new {@code "\."} to the node list.
         */
        private void addDecimalPointToNodes() {
            nodes.add(createDecimalPoint());
        }
        
        private Node createDecimalPoint() {
            return new EscapedSingleCharNode(RegexConstants.PERIOD);
        }
        
        /**
         * Add a new {@code "?"} to the node list.
         */
        private void addQuestionMarkToNodes() {
            nodes.add(new QuestionMarkNode());
        }
        
        private Pair<Integer,Integer> getRepetitionRange(RepetitionNode node) {
            Node child = node.getFirstChild();
            if (child instanceof IntegerNode) {
                int value = ((IntegerNode) child).getValue();
                return Pair.of(value, value);
            } else {
                IntegerRangeNode integerRange = (IntegerRangeNode) child;
                if (integerRange.isEndBounded()) {
                    return Pair.of(integerRange.getStart(), integerRange.getEnd());
                } else {
                    return Pair.of(integerRange.getStart(), null);
                }
            }
        }
        
        /**
         * Subtract one from the given range endpoints and return it.
         */
        private Pair<Integer,Integer> subtractOneFrom(Pair<Integer,Integer> range) {
            int left = range.getLeft() > 0 ? (range.getLeft() - 1) : 0;
            Integer right = range.getRight() == null ? null : (range.getRight() - 1);
            return Pair.of(left, right);
        }
        
        /**
         * Return a new repetition node created from the given range.
         */
        private RepetitionNode createRepetition(Pair<Integer,Integer> range) {
            if (Objects.equals(range.getLeft(), range.getRight())) {
                return new RepetitionNode(new IntegerNode(range.getLeft()));
            } else {
                return new RepetitionNode(new IntegerRangeNode(range.getLeft(), range.getRight()));
            }
        }
        
        /**
         * Return whether all remaining elements in the iterator can either occur zero times or match a zero.
         * 
         * @return true if the remaining pattern can be zero-length, or false otherwise.
         */
        private boolean remainingPatternCanBeZeroLength() {
            // Mark the original index so that we can reset the iterator before exiting this method.
            int originalIndex = iter.index();
            
            // Seek past all zero-matching elements.
            iter.seekPastZeroMatchingElements();
            
            boolean canBeZeroLength = true;
            while (iter.hasNext()) {
                Node next = iter.next();
                // If the next element can match zero, it could be a trailing zero that would get trimmed from encoded numbers.
                if (RegexUtils.matchesZero(next)) {
                    iter.seekPastQuantifiers();
                    iter.seekPastQuestionMarks();
                } else {
                    // If the next element cannot match zero, it could still occur zero times based on its quantifier (if present).
                    if (iter.hasNext() && iter.isNextQuantifier()) {
                        Node quantifier = iter.next();
                        if (quantifier instanceof OneOrMoreNode) {
                            // If the element is followed by +, it must occur at least once. Remaining pattern cannot be zero-length.
                            canBeZeroLength = false;
                            break;
                        } else if (quantifier instanceof RepetitionNode) {
                            // If the remaining element is not followed by repetition variation of {0} or {0,x}, it cannot occur zero times. Remaining pattern
                            // cannot be zero-length.
                            if (!RegexUtils.canOccurZeroTimes((RepetitionNode) quantifier)) {
                                canBeZeroLength = false;
                                break;
                            }
                        }
                    } else {
                        // If there is no quantifier, then the current element must occur. Remaining pattern cannot be zero-length.
                        canBeZeroLength = false;
                        break;
                    }
                }
            }
            iter.setIndex(originalIndex);
            return canBeZeroLength;
        }
        
        /**
         * Return whether only one more element (possibly quantified and/or optional) remains in the iterator.
         */
        private boolean remainingPatternHasOnlyOneMoreElement() {
            int originalIndex = iter.index();
            iter.next();
            iter.seekPastQuantifiers();
            iter.seekPastQuestionMarks();
            boolean hasOnlyOneMore = !iter.hasNext();
            iter.setIndex(originalIndex);
            return hasOnlyOneMore;
        }
    }
}
