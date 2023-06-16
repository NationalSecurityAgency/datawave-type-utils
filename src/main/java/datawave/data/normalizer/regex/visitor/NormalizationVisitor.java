package datawave.data.normalizer.regex.visitor;

import com.google.common.collect.ImmutableSet;
import datawave.data.normalizer.regex.AlternationNode;
import datawave.data.normalizer.regex.CharClassNode;
import datawave.data.normalizer.regex.CharRangeNode;
import datawave.data.normalizer.regex.EndAnchorNode;
import datawave.data.normalizer.regex.EscapedSingleCharNode;
import datawave.data.normalizer.regex.ExpressionNode;
import datawave.data.normalizer.regex.GTEOneBinFinder;
import datawave.data.normalizer.regex.IntegerNode;
import datawave.data.normalizer.regex.IntegerRangeNode;
import datawave.data.normalizer.regex.LTOneBinFinder;
import datawave.data.normalizer.regex.Node;
import datawave.data.normalizer.regex.NodeListIterator;
import datawave.data.normalizer.regex.OneOrMoreNode;
import datawave.data.normalizer.regex.OptionalNode;
import datawave.data.normalizer.regex.RegexConstants;
import datawave.data.normalizer.regex.RegexParser;
import datawave.data.normalizer.regex.RegexUtils;
import datawave.data.normalizer.regex.RepetitionNode;
import datawave.data.normalizer.regex.SingleCharNode;
import datawave.data.normalizer.regex.StartAnchorNode;
import datawave.data.normalizer.regex.ZeroOrMoreNode;
import datawave.data.type.util.NumericalEncoder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Implementation of {@link CopyVisitor} that returns a copy of a regex tree that represents a normalized version of the original regex tree. It is expected
 * that the previous tree had its groups expanded and flattened prior via {@link GroupAlternationsExpander#expand(Node)} and
 * {@link GroupFlattener#flatten(Node)}, and that any remaining groups remain because they were directly followed by {@code * + ?} or a repetition.
 * 
 * @see GroupAlternationsExpander
 * @see GroupFlattener
 */
public class NormalizationVisitor extends CopyVisitor {
    
    private static final Set<Class<? extends Node>> SIMPLE_NUMBER_TYPES = ImmutableSet.of(SingleCharNode.class, EscapedSingleCharNode.class,
                    StartAnchorNode.class, EndAnchorNode.class);
    private static final Pattern SIMPLE_NUMBER_REGEX_PATTERN = Pattern.compile("^\\^?(\\\\?-)?\\d*(\\\\\\.)?\\d+\\$?$");
    
    public static Node normalize(Node node) {
        if (node == null) {
            return null;
        }
        PrintVisitor.printToSysOut(node);
        NormalizationVisitor visitor = new NormalizationVisitor();
        return (Node) node.accept(visitor, null);
    }
    
    @Override
    public Object visitExpression(ExpressionNode node, Object data) {
        if (isParentOfAlternation(node)) {
            ExpressionNode copy = new ExpressionNode();
            copy.addChild((Node) node.getFirstChild().accept(this, data));
            return copy;
        } else if (isSimpleNumber(node)) {
            return normalizeNumber(node);
        } else {
            return EncodedExpressionBuilder.encode(node);
        }
    }
    
    /**
     * Return whether the given node has a single child that is an instance of {@link AlternationNode}.
     * 
     * @param node
     *            the node
     * @return true if the given node is a parent of an alternation, or false otherwise
     */
    private boolean isParentOfAlternation(Node node) {
        return node.getChildCount() == 1 && node.getChildAt(0) instanceof AlternationNode;
    }
    
    /**
     * Return whether the given node represents a simple number regex.
     * 
     * @param node
     *            the node
     * @return true if the node is a simple number regex, or false otherwise
     */
    private boolean isSimpleNumber(ExpressionNode node) {
        if (node.isAnyChildNotOf(SIMPLE_NUMBER_TYPES)) {
            return false;
        }
        String expression = StringVisitor.toString(node);
        return SIMPLE_NUMBER_REGEX_PATTERN.matcher(expression).matches();
    }
    
    /**
     * Create an encoded simple number regex from the given node. It is expected that the given node represents a simple number regex.
     * 
     * @param node
     *            the node to encode
     * @return the encoded node.
     */
    private Node normalizeNumber(ExpressionNode node) {
        // Create a number string from the node. Do not include backlashes or anchor characters.
        StringBuilder sb = new StringBuilder();
        for (Node child : node.getChildren()) {
            if (child instanceof EscapedSingleCharNode) {
                sb.append(((EscapedSingleCharNode) child).getCharacter());
            } else if (child instanceof SingleCharNode) {
                sb.append(((SingleCharNode) child).getCharacter());
            }
        }
        
        // Encode and escape the number.
        String encodedNumber = NumericalEncoder.encode(sb.toString());
        encodedNumber = RegexUtils.escapeEncodedNumber(encodedNumber);
        
        // Parse the number to a node.
        Node encodedNode = RegexParser.parse(encodedNumber);
        
        // If the original expression contained a starting anchor, include it in the encoded node.
        Node firstChild = node.getFirstChild();
        if (firstChild instanceof StartAnchorNode) {
            encodedNode.addChild(firstChild.shallowCopy(), 0);
        }
        
        // If the original expression contained an ending anchor, include it in the encoded node.
        Node lastChild = node.getLastChild();
        if (lastChild instanceof EndAnchorNode) {
            encodedNode.addChild(lastChild.shallowCopy());
        }
        
        return encodedNode;
    }
    
    private static class EncodedExpressionBuilder {
        
        private final Node original;
        private final boolean negative;
        private final NodeListIterator childrenIter;
        private Node encoded;
        private boolean onePlusBinFound;
        private boolean subOneBinFound;
        
        public static Node encode(Node node) {
            EncodedExpressionBuilder builder = new EncodedExpressionBuilder(node);
            return builder.build();
        }
        
        private EncodedExpressionBuilder(Node node) {
            this.original = node;
            this.negative = RegexUtils.isNegativeRegex(node);
            this.childrenIter = node.getChildrenIterator();
        }
        
        private Node build() {
            // Initialize the encoded node.
            this.encoded = new ExpressionNode();
            
            // Add the exponential bin information.
            addBinToEncoded();
            
            if (negative) {
                negateOriginal();
            }
            
            copyOriginalToEncoded();
            
            return this.encoded;
        }
        
        private void addBinToEncoded() {
            // If the regex is matching against negative numbers, the encoded regex must start with !. Otherwise, it must start with \+.
            if (negative) {
                this.encoded.addChild(new SingleCharNode(RegexConstants.EXCLAMATION_POINT));
            } else {
                this.encoded.addChild(new EscapedSingleCharNode(RegexConstants.PLUS));
            }
            
            // Determine what exponential bins should be included in the encoded expression.
            Pair<Integer,Integer> gteOneBinRange = GTEOneBinFinder.binRangeOf(this.original);
            onePlusBinFound = gteOneBinRange != null;
            System.out.println("gteOneBinRange: " + gteOneBinRange);
            
            Pair<Integer,Integer> ltOneBinRange = LTOneBinFinder.binRangeOf(this.original);
            subOneBinFound = ltOneBinRange != null;
            System.out.println("ltOneBinRange: " + ltOneBinRange);
            
            Node binNode = buildBinNode(gteOneBinRange, ltOneBinRange);
            System.out.println("Bin node: " + StringVisitor.toString(binNode));
            this.encoded.addChild(binNode);
            this.encoded.addChild(new SingleCharNode(RegexConstants.CAPITAL_E));
        }
        
        private Node buildBinNode(Pair<Integer,Integer> gteOneBinRange, Pair<Integer,Integer> ltOneBinRange) {
            Function<Integer,Character> binFunction = negative ? NumericalEncoder::getNegativeBin : NumericalEncoder::getPositiveBin;
            
            if (gteOneBinRange == null) {
                return buildBinFromSingleRange(ltOneBinRange, binFunction, true);
            } else if (ltOneBinRange == null) {
                return buildBinFromSingleRange(gteOneBinRange, binFunction, false);
            } else {
                CharClassNode charClass = new CharClassNode();
                Node onePlusBin = buildBinFromSingleRange(gteOneBinRange, binFunction, false);
                charClass.addChild(onePlusBin instanceof SingleCharNode ? onePlusBin : onePlusBin.getFirstChild());
                
                Node subOneBin = buildBinFromSingleRange(ltOneBinRange, binFunction, true);
                charClass.addChild(subOneBin instanceof SingleCharNode ? subOneBin : subOneBin.getFirstChild());
                return charClass;
            }
        }
        
        private Node buildBinFromSingleRange(Pair<Integer,Integer> binRange, Function<Integer,Character> binFunction, boolean invert) {
            if (binRange.getLeft().equals(binRange.getRight())) {
                return new SingleCharNode(binFunction.apply(binRange.getLeft()));
            } else {
                CharClassNode charClass = new CharClassNode();
                char left = binFunction.apply(binRange.getLeft());
                char right = binFunction.apply(binRange.getRight());
                if (invert) {
                    charClass.addChild(new CharRangeNode(right, left));
                } else {
                    charClass.addChild(new CharRangeNode(left, right));
                }
                return charClass;
            }
        }
        
        private void negateOriginal() {
            
        }
        
        private void copyOriginalToEncoded() {
            // If we determined that the regex is matching against negative numbers, skip over the negative sign.
            if (negative) {
                childrenIter.next();
            }
            
            // If the regex has a leading quantified character, it must be specially handled when copying it to the encoded tree.
            childrenIter.next();
            if (childrenIter.hasNext() && childrenIter.isNextInstanceOfAny(RegexConstants.QUANTIFIER_TYPES)) {
                copyLeadingQuantifiedCharacter();
            } else {
                // If there is not a leading quantified character, step the iterator back one to undo the previous call to next().
                childrenIter.previous();
            }
            
            // If the last character is 'E', we have not added an escaped decimal point to the encoded tree yet.
            boolean addedDecimalPoint = !RegexUtils.isChar(encoded.getLastChild(), RegexConstants.CAPITAL_E);
            
            // If no bin range was found for numbers equal to or greater than one, skip all leading zeroes.
            if (!onePlusBinFound) {
                childrenIter.seekPastZeroMatchingElements();
                Node peeked = childrenIter.peekNext();
                if (RegexUtils.isDecimalPoint(peeked)) {
                    childrenIter.next();
                    childrenIter.seekPastZeroOnlyElements();
                }
            }
            
            // Copy over relevant nodes.
            while (childrenIter.hasNext()) {
                Node next = childrenIter.next();
                // If the next node is not the decimal point, copy it to the encoded expression.
                if (!RegexUtils.isDecimalPoint(next)) {
                    encoded.addChild(copy(next));
                    if (!addedDecimalPoint && childrenIter.hasNext()) {
                        this.encoded.addChild(new EscapedSingleCharNode(RegexConstants.PERIOD));
                        addedDecimalPoint = true;
                    }
                }
            }
            
            // If the last child in the encoded tree is a decimal point, we had a single character regex. Remove the trailing decimal point.
            if (RegexUtils.isDecimalPoint(this.encoded.getLastChild())) {
                this.encoded.removeLastChild();
            } else {
                // Check for a unique case where there is only one trailing element after the decimal point that can possibly occur zero times. In this case, we
                // need to add a ? after the decimal point to allow matches against numbers that don't have a decimal point when encoded.
                NodeListIterator iter = this.encoded.getChildrenIterator();
                // Seek past to directly after the decimal point, and mark where the index for it is.
                iter.seekPastDecimalPoint();
                int decimalPointIndex = iter.index();
                // If all remaining elements in the iter after the
                if (allRemainingElementsCanOccurZeroTimes(iter)) {
                    this.encoded.addChild(new OptionalNode(), decimalPointIndex);
                }
            }
        }
        
        /**
         * Return whether all remaining elements in the given iter can occur zero times.
         * 
         * @param iter
         *            the iter
         * @return true if all remaining elements can occur zero times, or false otherwise
         */
        private boolean allRemainingElementsCanOccurZeroTimes(NodeListIterator iter) {
            while (iter.hasNext()) {
                iter.next();
                // If the current element has a quantifier, see if the quantifier allows for no occurrence.
                if (iter.isNextQuantifier()) {
                    Node quantifier = iter.next();
                    switch (quantifier.getType()) {
                        case ZERO_OR_MORE:
                            // We encountered a *. Skip over any succeeding optional if present.
                            iter.seekPastOptional();
                            break;
                        case REPETITION:
                            // We encountered a repetition. We shouldn't have any {0} at this point, but could have {0,} or {0,x}.
                            Node repetition = quantifier.getFirstChild();
                            if (!(repetition instanceof IntegerRangeNode) || ((IntegerRangeNode) repetition).getStart() != 0) {
                                return false;
                            }
                            iter.seekPastOptional();
                            break;
                        default:
                            return false;
                    }
                } else if (iter.isNextOptional()) {
                    // If the current element is optional, seek past the optional element.
                    iter.seekPastOptional();
                } else {
                    // The current element cannot occur zero times.
                    return false;
                }
            }
            return true;
        }
        
        /**
         * Copy leading quantified characters to the encoded regex. This method handles cases where a regex starts with a quantified character, for example:
         * {@code ".*"}, {@code ".+"}, {@code "3+"}, {@code "3{5}"}, etc. Because one occurrence of the repeated character will always be before the decimal
         * point in encoded numbers, we need to repeat the character and quantifier after the decimal point, but with one less occurrence.
         */
        private void copyLeadingQuantifiedCharacter() {
            Node character = childrenIter.peekPrevious();
            Node quantifier = childrenIter.next();
            Node newQuantifier = null;
            
            // By default, we should always repeat the character after the decimal point except in the case where the quantifier is {1}.
            boolean repeatCharacterAfterDecimalPoint = true;
            
            switch (quantifier.getType()) {
                case ZERO_OR_MORE:
                case ONE_OR_MORE:
                    // In both cases of * and + being specified after a character, we can allow repeats of the character after the decimal point zero or more
                    // times.
                    newQuantifier = new ZeroOrMoreNode();
                    break;
                case REPETITION:
                    Node child = quantifier.getFirstChild();
                    if (child instanceof IntegerNode) {
                        int value = ((IntegerNode) child).getValue();
                        if (value == 1) {
                            // If a user has specified {1} for some odd reason, we do not need to repeat the character after the decimal point at all.
                            repeatCharacterAfterDecimalPoint = false;
                        } else if (value > 2) {
                            // If a user has specified {3} or higher, subtract the target repetition by one. This is how many times the character must repeat
                            // after the decimal point. In the case of {2}, we simply need to repeat the character after the decimal point, but do not need a
                            // quantifier.
                            newQuantifier = new RepetitionNode();
                            newQuantifier.addChild(new IntegerNode((value - 1)));
                        }
                    } else {
                        IntegerRangeNode rangeNode = (IntegerRangeNode) child;
                        
                        // Subtract one from the start and end of the repetition range. Do not allow start to be less than 0, and keep unbounded ends.
                        int newStart = rangeNode.getStart() == 0 ? 0 : rangeNode.getStart() - 1;
                        Integer newEnd = rangeNode.isEndBounded() ? rangeNode.getEnd() - 1 : null;
                        
                        if (newStart == 0 && newEnd == null) {
                            // If we now have a range of {0,} for the character occurrence after the decimal point, this is equivalent to putting a * after
                            // the character.
                            newQuantifier = new ZeroOrMoreNode();
                            // If we now have a range of {1,} for the character occurrence after the decimal point, this is equivalent to putting a + after
                            // the character.
                        } else if (newStart == 1 && newEnd == null) {
                            newQuantifier = new OneOrMoreNode();
                        } else if (newStart == 0 && newEnd == 1) {
                            // If we now have a range of {0,1} for the character occurrence after the decimal point, this is equivalent to putting an optional
                            // marker after the character.
                            newQuantifier = new OptionalNode();
                            
                        } else {
                            // Typically we will put an edited repetition range for the character occurrence after the decimal point.
                            newQuantifier = new RepetitionNode();
                            newQuantifier.addChild(new IntegerRangeNode(newStart, newEnd));
                        }
                    }
            }
            
            // Copy the character and put an escaped decimal point after it.
            encoded.addChild(copy(character));
            encoded.addChild(new EscapedSingleCharNode(RegexConstants.PERIOD));
            
            // If we need to repeat the character after the decimal point, do so.
            if (repeatCharacterAfterDecimalPoint) {
                encoded.addChild(copy(character));
                
                // If we need to add a quantifier after the character repeat, do so.
                if (newQuantifier != null) {
                    encoded.addChild(newQuantifier);
                    if (childrenIter.hasNext() && childrenIter.isNextInstanceOf(OptionalNode.class)) {
                        childrenIter.next();
                        // It is possible for the quantifier to be an optional node. In this case, do not copy the next optional node.
                        if (!(quantifier instanceof OptionalNode)) {
                            encoded.addChild(new OptionalNode());
                        }
                    }
                }
            }
        }
        
    }
    
}
