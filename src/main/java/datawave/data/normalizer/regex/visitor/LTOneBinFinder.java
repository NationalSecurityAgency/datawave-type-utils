package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.AnyCharNode;
import datawave.data.normalizer.regex.Node;
import datawave.data.normalizer.regex.NodeType;
import datawave.data.normalizer.regex.RegexUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Implementation of {@link BinFinder} that finds the range of exponential bins that a regex pattern should match against for numbers less than one.
 */
public class LTOneBinFinder extends BinFinder {
    
    private static final int MAX_BIN = 26;
    private static final int MIN_BIN = 1;
    private static final int INITIAL_ENDPOINT_VALUE = 0;
    
    public static Pair<Integer,Integer> binRangeOf(Node node) {
        LTOneBinFinder calculator = new LTOneBinFinder(node);
        return calculator.getBinRange();
    }
    
    protected LTOneBinFinder(Node node) {
        super(node, MIN_BIN, MAX_BIN, INITIAL_ENDPOINT_VALUE);
    }
    
    @Override
    protected Pair<Integer,Integer> getBinRange() {
        if (decimalPointIndex == -1) {
            calculateRangeWithoutDecimalPoint();
        } else {
            calculateRangeWithDecimalPoint();
        }
        normalizeRange();
        
        // When retrieving bins for numbers less than one, the bin values must be negative. Negate the endpoints.
        lower = -lower;
        upper = -upper;
        
        return getEndpoints();
    }
    
    /**
     * Calculate the bin range for a pattern that has no decimal point specified in it.
     */
    private void calculateRangeWithoutDecimalPoint() {
        int firstWildcardIndex = node.indexOf(AnyCharNode.class);
        
        // If there is no wildcard present in the regex, the regex does not need a bin range for numbers less than one.
        if (firstWildcardIndex == -1) {
            return;
        }
        
        // If the wildcard is not the first element in the pattern, see if it's possible to have leading zeros up to the first wildcard. If not, the pattern
        // will not match against numbers less than one and does not need a bin range for numbers less than one.
        if (childrenIter.index() != firstWildcardIndex) {
            while (childrenIter.index() != firstWildcardIndex) {
                Node next = childrenIter.peekNext();
                // We found an element that cannot match zero before the wildcard. Return early.
                if (!RegexUtils.matchesZero(next)) {
                    return;
                } else {
                    // We found an element that can match zero. Move the iterator forward, and skip any quantifiers or optional characters.
                    childrenIter.next();
                    childrenIter.seekPastQuantifier();
                    childrenIter.seekPastQuantifier();
                }
            }
        }
        
        while (childrenIter.hasNext()) {
            int nextIndex = childrenIter.index();
            Node next = childrenIter.next();
            switch (next.getType()) {
                case ANY_CHAR:
                    // Mark that we've seen a wildcard, and fall through to the operations below the SINGLE_CHAR case.
                    // Do not add a break after this statement.
                    this.wildcardSeen = true;
                case CHAR_CLASS:
                case DIGIT_CHAR_CLASS:
                case SINGLE_CHAR:
                    // If the current character could be a leading zero, update the bin range.
                    if (RegexUtils.matchesZero(next)) {
                        if (childrenIter.hasNext() && childrenIter.isNextQuantifier()) {
                            updateRangeWithQuantifier();
                        } else {
                            if (wildcardSeen) {
                                // Check if this is the first time we've seen the wildcard. If there is nothing else after it, then we do not need a bin range
                                // for
                                // numbers less than one. Otherwise, we should increment the lower bound.
                                if (nextIndex == firstWildcardIndex) {
                                    if (childrenIter.hasNext()) {
                                        incrementLower();
                                    } else {
                                        return;
                                    }
                                }
                            } else {
                                // If no wildcard has been seen, increment the lower bound.
                                incrementLower();
                            }
                            
                            // If no quantifier was specified, increment the upper bound.
                            incrementUpper();
                        }
                    } else {
                        // There are no more leading zeroes, and nothing more to do.
                        return;
                    }
            }
        }
    }
    
    /**
     * Calculate the bin range for a pattern with a decimal point in it.
     */
    private void calculateRangeWithDecimalPoint() {
        // Seek past children that can match the character '0'. If the next child after this is not the decimal point, then the regex expression will not
        // match against numbers less than one.
        childrenIter.seekPastZeroMatchingElements();
        if (childrenIter.index() != decimalPointIndex) {
            return;
        }
        
        // Skip over the decimal point to the next character.
        childrenIter.next();
        // We will at least have the minimum bin range possible.
        incrementUpper();
        incrementLower();
        
        // For each possible leading zero after the decimal point, update the bin range.
        while (childrenIter.hasNext()) {
            Node next = childrenIter.next();
            // If next can be a leading zero, update the bin range/
            if (RegexUtils.matchesZero(next)) {
                if (next.getType() == NodeType.ANY_CHAR) {
                    this.wildcardSeen = true;
                }
                if (childrenIter.isNextQuantifier()) {
                    updateRangeWithQuantifier();
                } else {
                    if (!wildcardSeen) {
                        incrementLower();
                    }
                    incrementUpper();
                }
            } else {
                // If next cannot possibly be a leading zero, there is nothing more to do.
                break;
            }
        }
    }
}
