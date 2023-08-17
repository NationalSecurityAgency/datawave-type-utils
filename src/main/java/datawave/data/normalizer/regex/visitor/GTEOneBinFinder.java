package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.Node;
import datawave.data.normalizer.regex.RegexUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Implementation of {@link BinFinder} that finds the range of exponential bins that a regex pattern should match against for numbers equal to or greater than
 * one.
 */
public class GTEOneBinFinder extends BinFinder {
    
    private static final int MIN_BIN = 0;
    private static final int MAX_BIN = 25;
    private static final int INITIAL_ENDPOINT_VALUE = -1;
    
    public static Pair<Integer,Integer> binRangeOf(Node node) {
        GTEOneBinFinder calculator = new GTEOneBinFinder(node);
        return calculator.getBinRange();
    }
    
    protected GTEOneBinFinder(Node node) {
        super(node, MIN_BIN, MAX_BIN, INITIAL_ENDPOINT_VALUE);
    }
    
    @Override
    protected Pair<Integer,Integer> getBinRange() {
        calculateRange();
        normalizeRange();
        return getEndpoints();
    }
    
    private void calculateRange() {
        // Skip any leading zero elements that only match a zero character.
        childrenIter.seekPastZeroOnlyElements();
        
        // The following variables are used for tracking a separate lower bound value that skips any leading possible zeros.
        boolean leadingZeroMatchingElementSeen = RegexUtils.matchesZero(childrenIter.peekNext());
        int minLowerBoundAfterLeadingZeros = -1;
        boolean nonZeroMatchingElementSeen = false;
        
        while (childrenIter.hasNext() && !(childrenIter.index() == decimalPointIndex)) {
            Node next = childrenIter.next();
            switch (next.getType()) {
                case ANY_CHAR:
                    // Mark that we've seen a wildcard, and fall through to the operations below the SINGLE_CHAR case.
                    // Do not add a break after this statement.
                    this.wildcardSeen = true;
                case CHAR_CLASS:
                case DIGIT_CHAR_CLASS:
                case SINGLE_CHAR:
                    // If there was a leading possible zero, check if we should increment our separate lower bound value.
                    if (leadingZeroMatchingElementSeen) {
                        if (nonZeroMatchingElementSeen) {
                            // Do not keep incrementing the lower bound after the first wildcard has been seen.
                            if (!wildcardSeen) {
                                minLowerBoundAfterLeadingZeros++;
                            }
                        } else {
                            // If we have not seen an element that cannot possibly match zero yet, check if the current element satisfies that criteria.
                            if (!RegexUtils.matchesZero(next)) {
                                nonZeroMatchingElementSeen = true;
                                minLowerBoundAfterLeadingZeros++;
                            }
                        }
                    }
                    if (childrenIter.hasNext() && childrenIter.isNextQuantifier()) {
                        // If a quantifier was specified, increment the upper and lower bound based on the quantifier type.
                        updateRangeWithQuantifier();
                    } else {
                        // If no quantifier was specified, increment the upper bound by one.
                        incrementUpper();
                        // Only increment the lower bound by one if it is not locked.
                        if (isLowerUnlocked()) {
                            incrementLower();
                        }
                    }
                    break;
                case ESCAPED_SINGLE_CHAR:
                    throw new IllegalArgumentException("Encountered unsupported escaped character " + StringVisitor.toString(next));
                default: {
                    // Do not modify the upper and lower bound. Move to the next child.
                }
            }
        }
        
        // If we had a leading possible zero element, and we have seen at least one element that cannot match zero, set the lower bound to the lowest of the two
        // lower bound calculations: the lower bound after skipping all possible leading zeros, or the lower bound up to the first wildcard.
        if (leadingZeroMatchingElementSeen && nonZeroMatchingElementSeen) {
            if (minLowerBoundAfterLeadingZeros < lower) {
                lower = minLowerBoundAfterLeadingZeros;
            }
        }
    }
}
