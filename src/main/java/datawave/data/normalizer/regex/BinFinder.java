package datawave.data.normalizer.regex;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Abstract class for {@link LTOneBinFinder} and {@link GTEOneBinFinder} with common properties and functionality.
 */
abstract class BinFinder {
    
    // The original node.
    protected final Node node;
    
    // An iterator for the node's children.
    protected final NodeListIterator childrenIter;
    
    // The index of the decimal point in the node's children, possibly -1.
    protected final int decimalPointIndex;
    
    // The smallest bin value.
    protected final int minBin;
    
    // The highest bin value.
    protected final int maxBin;
    
    // The initial value for the lower and upper endpoints.
    protected final int initialEndpointValue;
    
    // The current lower end of the bin range.
    protected int lower;
    
    // The current upper end of the bin range.
    protected int upper;
    
    // Whether a wildcard has been seen yet while iterating over the node's children.
    protected boolean wildcardSeen = false;
    
    protected BinFinder(Node node, int minBin, int maxBin, int initialEndpointValue) {
        this.node = node;
        this.decimalPointIndex = RegexUtils.getDecimalPointIndex(node);
        this.minBin = minBin;
        this.maxBin = maxBin;
        this.initialEndpointValue = initialEndpointValue;
        this.childrenIter = node.getChildrenIterator();
        
        // Set the initial end point values.
        this.lower = initialEndpointValue;
        this.upper = initialEndpointValue;
        
        // If the first child is a hyphen, skip over it and start at the next child.
        if (RegexUtils.isChar(node.getFirstChild(), RegexConstants.HYPHEN)) {
            childrenIter.next();
        }
    }
    
    protected abstract Pair<Integer,Integer> getBinRange();
    
    /**
     * Increment lower by one.
     */
    protected void incrementLower() {
        lower++;
    }
    
    /**
     * Increment lower by the given value.
     * 
     * @param value
     *            the value
     */
    protected void incrementLower(int value) {
        lower += value;
    }
    
    /**
     * Increment upper by one.
     */
    protected void incrementUpper() {
        upper++;
    }
    
    /**
     * Increment upper by the given value.
     * 
     * @param value
     *            the value
     */
    protected void incrementUpper(int value) {
        upper += value;
    }
    
    /**
     * Set upper to the max bin value.
     */
    protected void setUpperToMax() {
        upper = maxBin;
    }
    
    /**
     * Normalize the endpoints to be within the min and max bin if they were updated.
     */
    protected void normalizeRange() {
        // Do not normalize if both the upper and lower are the initial endpoint value. This indicates that a valid bin range was not found.
        if (lower != initialEndpointValue || upper != initialEndpointValue) {
            // Normalize the bin range to be within a valid bin range. If the lower bound is less than the min bin, set it to the min bin. If it is greater than
            // the max bin, set it to the max bin.
            if (lower < minBin) {
                lower = minBin;
            } else if (lower > maxBin) {
                lower = maxBin;
            }
            
            // If the upper bound is greater than the max bin, set it to the max bin.
            if (upper > maxBin) {
                upper = maxBin;
            }
        }
    }
    
    /**
     * Return a {@link Pair} with the lower and upper bin range endpoints, or null if no valid bin range was found.
     * 
     * @return the bin range
     */
    protected Pair<Integer,Integer> getEndpoints() {
        if (lower != initialEndpointValue || upper != initialEndpointValue) {
            return Pair.of(lower, upper);
        } else {
            return null;
        }
    }
    
    /**
     * Return whether the lower bound is considered unlocked and modifiable. The lower bound is considered unlocked if we have not traversed to a wildcard yet
     * in the regex, or if there is a defined decimal point in the regex.
     *
     * @return true if the lower bound can be modified, or false otherwise
     */
    protected boolean isLowerUnlocked() {
        return !this.wildcardSeen || this.decimalPointIndex != -1;
    }
    
    /**
     * Update lower and upper based off the quantities read from the next quantifier.
     */
    protected void updateRangeWithQuantifier() {
        Node quantifier = childrenIter.next();
        switch (quantifier.getType()) {
            case REPETITION:
                // In the case of a repetition node, we may have an IntegerNode or IntegerRangeNode child.
                Node child = quantifier.getFirstChild();
                if (child instanceof IntegerNode) {
                    // Increment both the upper and lower bound by the repetition value.
                    int value = ((IntegerNode) child).getValue();
                    // Only increment the lower bound if it is not locked.
                    if (isLowerUnlocked()) {
                        incrementLower(value);
                    }
                    // Increment the upper bound by the value.
                    incrementUpper(value);
                } else {
                    IntegerRangeNode rangeNode = (IntegerRangeNode) child;
                    // Only increment the lower bound if it is not locked.
                    if (isLowerUnlocked()) {
                        incrementLower(rangeNode.getStart());
                    }
                    // If the end of the range has a bound, increment the upper bound by the end bound. Otherwise, set the upper bound to the max.
                    if (rangeNode.isEndBounded()) {
                        incrementUpper(rangeNode.getEnd());
                    } else {
                        setUpperToMax();
                    }
                }
                break;
            case ZERO_OR_MORE:
                // Set the upper to the max. Do not modify the lower bound.
                setUpperToMax();
                break;
            case ONE_OR_MORE:
                // Set the upper bound to the max.
                setUpperToMax();
                // Only increment the lower bound if it is not locked.
                if (isLowerUnlocked()) {
                    incrementLower();
                }
                break;
        }
        
        // If the node after the quantifier node is an optional node, skip over it.
        childrenIter.seekPastOptional();
    }
}
