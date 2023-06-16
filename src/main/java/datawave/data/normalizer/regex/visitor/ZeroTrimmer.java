package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.AnyCharNode;
import datawave.data.normalizer.regex.CharClassNode;
import datawave.data.normalizer.regex.CharRangeNode;
import datawave.data.normalizer.regex.EncodedPatternNode;
import datawave.data.normalizer.regex.GroupNode;
import datawave.data.normalizer.regex.IntegerNode;
import datawave.data.normalizer.regex.IntegerRangeNode;
import datawave.data.normalizer.regex.Node;
import datawave.data.normalizer.regex.NodeListIterator;
import datawave.data.normalizer.regex.NodeType;
import datawave.data.normalizer.regex.OneOrMoreNode;
import datawave.data.normalizer.regex.OptionalNode;
import datawave.data.normalizer.regex.RegexConstants;
import datawave.data.normalizer.regex.RegexUtils;
import datawave.data.normalizer.regex.RepetitionNode;
import datawave.data.normalizer.regex.SingleCharNode;
import datawave.data.normalizer.regex.ZeroOrMoreNode;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link CopyVisitor} that trims and consolidates leading zeros for partially encoded regex patterns.
 */
public class ZeroTrimmer extends CopyVisitor {
    
    /**
     * Return a copy of the node tree with all leading zeros for partially encoded regex patterns either trimmed and/or consolidated.
     * 
     * @param node
     *            the node
     * @return the trimmed tree
     */
    public static Node trim(Node node) {
        if (node == null) {
            return null;
        }
        ZeroTrimmer visitor = new ZeroTrimmer();
        return (Node) node.accept(visitor, null);
    }
    
    @Override
    public Object visitEncodedPattern(EncodedPatternNode node, Object data) {
        EncodedPatternNode trimmed = new EncodedPatternNode();
        // Create a new node and add each child up to (inclusively) the 'E' character.
        int startOfRemainingNodes = 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            Node child = node.getChildAt(i);
            trimmed.addChild(copy(child));
            if (RegexUtils.isChar(child, RegexConstants.CAPITAL_E)) {
                startOfRemainingNodes = i + 1;
                break;
            }
        }
        
        // Copy the remaining children into a separate list. This list will be modified as zeros are trimmed.
        List<Node> nodes = new ArrayList<>();
        for (int i = startOfRemainingNodes; i < node.getChildCount(); i++) {
            Node child = node.getChildAt(i);
            // At this point we no longer need to keep the original decimal point. A new decimal point will be added later in the correct spot.
            if (!RegexUtils.isDecimalPoint(child)) {
                nodes.add(copy(child));
            }
        }
        
        // Check if the remaining children represent a single regex element. If so, no trimming is required.
        if (isSingleElementPattern(nodes)) {
            trimmed.addChildren(nodes);
            return trimmed;
        }
        
        // Trim any leading zeros.
        nodes = trimLeadingZeroOnlyElements(nodes);
        
        // Consolidate any possible leading zeros.
        nodes = consolidatePossibleLeadingZeros(nodes);
        
        // Trim any trailing zeros.
        nodes = trimTrailingZeroOnlyElements(nodes);
        
        // Consolidate any possible trailing zeros.
        nodes = consolidatePossibleTrailingZeros(nodes);
        
        // The nodes are in reverse order after trimming trailing zeros. Correct the order.
        Collections.reverse(nodes);
        
        // Add the new nodes to the node to return.
        trimmed.addChildren(nodes);
        return trimmed;
    }
    
    /**
     * Return true if the given list consists only of one regex element that may or may not be followed by a quantifier or optional.
     * 
     * @param nodes
     *            the nodes
     * @return true if the list consists of a single element pattern, or false otherwise
     */
    private boolean isSingleElementPattern(List<Node> nodes) {
        NodeListIterator iter = new NodeListIterator(nodes);
        iter.next();
        iter.seekPastQuantifier();
        iter.seekPastOptional();
        return !iter.hasNext();
    }
    
    /**
     * Trim all leading nodes that only match zero. Trimming will stop once the first element that can match something other than zero is seen.
     * 
     * @param nodes
     *            the nodes
     * @return a list of trimmed nodes
     */
    private List<Node> trimLeadingZeroOnlyElements(List<Node> nodes) {
        NodeListIterator iter = new NodeListIterator(nodes);
        while (iter.hasNext()) {
            Node next = iter.peekNext();
            // If the next element matches zero only, skip past it, and any quantifiers and/or optionals after it.
            if (RegexUtils.matchesZeroOnly(next)) {
                iter.next();
                iter.seekPastQuantifier();
                iter.seekPastOptional();
            } else {
                break;
            }
        }
        
        // If no leading zeros were seen, return the original list, otherwise return a sublist.
        return iter.index() == 0 ? nodes : new ArrayList<>(nodes.subList(iter.index(), nodes.size()));
    }
    
    /**
     * Return a list with all possible leading zeros consolidated, and any elements made optional as needed.
     * 
     * @param nodes
     *            the nodes to consolidate
     * @return a list of consolidated nodes
     */
    private List<Node> consolidatePossibleLeadingZeros(List<Node> nodes) {
        // If the first node cannot match zero, there is nothing further to do. Return the entire list.
        if (!RegexUtils.matchesZero(nodes.get(0))) {
            return nodes;
        }
        
        // Iterate through each child.
        NodeListIterator iter = new NodeListIterator(nodes);
        List<Node> consolidated = new ArrayList<>();
        while (iter.hasNext()) {
            // Do not call next until we know the next node can match zero.
            Node next = iter.peekNext();
            // The next node can match zero. Call next, and call the specific consolidation method based on whether the node can match only zero, or other
            // numbers.
            if (RegexUtils.matchesZero(next)) {
                if (RegexUtils.matchesZeroOnly(next)) {
                    consolidated.addAll(consolidateLeadingMatchesZeroOnly(iter));
                } else {
                    consolidated.addAll(consolidateLeadingMatchesZero(iter));
                }
            } else {
                break;
            }
        }
        
        // Add the remaining nodes to the list to return.
        while (iter.hasNext()) {
            consolidated.add(iter.next());
        }
        return consolidated;
    }
    
    /**
     * Consolidate any leading zeros that can possibly match zero.
     * 
     * @param iter
     *            the iterator
     * @return the consolidated nodes.
     */
    private List<Node> consolidateLeadingMatchesZero(NodeListIterator iter) {
        List<Node> nodes = new ArrayList<>();
        while (iter.hasNext()) {
            // Do not call next until we know the next node can match zero.
            Node next = iter.peekNext();
            // The next node can match zero. The first call to next should always return an element that can match zero, but not only zero.
            if (RegexUtils.matchesZero(next)) {
                iter.next();
                // If the node is followed by a quantifier and/or optional, add them too.
                if (iter.hasNext() && iter.isNextQuantifier()) {
                    nodes.add(next);
                    nodes.add(iter.next());
                    if (iter.hasNext() && iter.isNextOptional()) {
                        nodes.add(iter.next());
                    }
                } else {
                    // If the node is not followed by a quantifier, check if the current node is a zero. If so, remove the possibility for the character class
                    // to match against zero.
                    if (next instanceof CharClassNode) {
                        nodes.add(removeZeroFromCharClass(next));
                    } else {
                        nodes.add(next);
                    }
                    // Make the node optional since it can possibly be a leading zero, and thus must be optional.
                    nodes.add(new OptionalNode());
                }
                
                // If there are any elements directly after the current element that only match zero, consolidate then and add the result.
                if (iter.hasNext() && RegexUtils.matchesZeroOnly(iter.peekNext())) {
                    nodes.addAll(consolidateLeadingMatchesZeroOnly(iter));
                }
            } else {
                // The next element cannot match zero. Nothing more to do.
                break;
            }
        }
        return nodes;
    }
    
    /**
     * Consolidate the next consecutive elements that can only match zero.
     * 
     * @param iter
     *            the iterator
     * @return a list of the consolidated nodes
     */
    private List<Node> consolidateLeadingMatchesZeroOnly(NodeListIterator iter) {
        // We need to track the minimum and maximum times a leading zero can occur.
        int minZeroCount = 0;
        int maxZeroCount = 0;
        
        while (iter.hasNext()) {
            // Do not call next until we've confirmed the next node only matches zero.
            Node next = iter.peekNext();
            if (RegexUtils.matchesZeroOnly(next)) {
                // Explicitly call next now.
                iter.next();
                // If the zero has a quantifier, extract the quantifier range.
                if (iter.isNextQuantifier()) {
                    Pair<Integer,Integer> quantifierRange = RegexUtils.getQuantifierRange(iter.next());
                    // Increment the lower bound.
                    minZeroCount += quantifierRange.getLeft();
                    if (maxZeroCount != -1) {
                        // If the quantifier range has no defined upper bound, that is equivalent to unlimited. Set the max bound to -1 to ensure it is not
                        // changed.
                        if (quantifierRange.getRight() == null) {
                            maxZeroCount = -1;
                        } else {
                            // Otherwise increment the upper bound.
                            maxZeroCount += quantifierRange.getRight();
                        }
                    }
                } else {
                    // The zero does not have a quantifier. Increment the min count by one, and increment the max count only if we have not yet determined that
                    // the max should be considered unlimited.
                    minZeroCount++;
                    if (maxZeroCount != -1) {
                        maxZeroCount++;
                    }
                }
                // Skip any optional if present.
                iter.seekPastOptional();
            } else {
                // If the next node does not only match zero, stop iterating.
                break;
            }
        }
        
        List<Node> nodes = new ArrayList<>();
        // If the min and max are both 1, return 0?
        if (minZeroCount == 1 && maxZeroCount == 1) {
            nodes.add(new SingleCharNode(RegexConstants.ZERO));
            nodes.add(new OptionalNode());
        } else {
            // Otherwise we need return 0 followed by a quantifier inside an optional group.
            GroupNode groupNode = new GroupNode();
            groupNode.addChild(new SingleCharNode(RegexConstants.ZERO));
            
            if (maxZeroCount == -1 && minZeroCount < 2) {
                if (minZeroCount == 0) {
                    // Return (0*)?
                    groupNode.addChild(new ZeroOrMoreNode());
                } else if (minZeroCount == 1) {
                    // Return (0+)?
                    groupNode.addChild(new OneOrMoreNode());
                }
            } else {
                RepetitionNode repetition = new RepetitionNode();
                if (minZeroCount == maxZeroCount) {
                    // Return (0{x})?
                    IntegerNode integer = new IntegerNode(minZeroCount);
                    repetition.addChild(integer);
                } else {
                    // Return (0{x,y})? or (0{x,})? if unlimited max.
                    IntegerRangeNode integerRange = new IntegerRangeNode();
                    integerRange.setStart(minZeroCount);
                    if (maxZeroCount != -1) {
                        integerRange.setEnd(maxZeroCount);
                    }
                    repetition.addChild(integerRange);
                }
                
                groupNode.addChild(repetition);
            }
            nodes.add(groupNode);
            // Ensure the group is optional.
            nodes.add(new OptionalNode());
        }
        
        return nodes;
    }
    
    /**
     * Trim all trailing nodes that explicitly only match zero. Trimming will stop once the first element that can match something other than zero is seen.
     * 
     * @param nodes
     *            the nodes
     * @return a list of trimmed nodes
     */
    private List<Node> trimTrailingZeroOnlyElements(List<Node> nodes) {
        // Reverse the list so that we iterate over it in reverse order.
        Collections.reverse(nodes);
        NodeListIterator iter = new NodeListIterator(nodes);
        
        while (iter.hasNext()) {
            // Keep a record of the current index so that we can reset it once we find an element that cannot match zero.
            int lastIndex = iter.index();
            // Skip past any optionals or quantifiers that are before the element. Remember, we're iterating over the list in reverse order.
            iter.seekPastOptional();
            iter.seekPastQuantifier();
            Node next = iter.peekNext();
            // If the next element matches zero only, skip past it.
            if (RegexUtils.matchesZeroOnly(next)) {
                iter.next();
            } else {
                // Reset the index to the non-zero matching element.
                iter.setIndex(lastIndex);
                break;
            }
        }
        
        // If no trailing zeros were seen, return the original list, otherwise return a sublist.
        return iter.index() == 0 ? nodes : new ArrayList<>(nodes.subList(iter.index(), nodes.size()));
    }
    
    /**
     * Return a list with all possible trailing zeros consolidated, and any elements made optional as needed.
     * 
     * @param nodes
     *            the nodes to consolidate
     * @return a list of consolidated nodes
     */
    private List<Node> consolidatePossibleTrailingZeros(List<Node> nodes) {
        // List of consolidated nodes.
        List<Node> consolidated = new ArrayList<>();
        NodeListIterator iter = new NodeListIterator(nodes);
        
        // Check if the pattern ends with '.+' or '.+?'. In this case, the '.+' must become a '.*' to allow for matching against numbers that had trailing zeros
        // that were subsequently trimmed when encoded.
        if (iter.hasNext()) {
            int lastIndex = iter.index();
            Node optional = iter.isNextOptional() ? iter.next() : null;
            Node quantifier = iter.isNextQuantifier() ? iter.next() : null;
            Node next = iter.next();
            // if the last element of the pattern is .+, convert it to .*.
            if (next.getType() == NodeType.ANY_CHAR && quantifier != null && quantifier.getType() == NodeType.ONE_OR_MORE) {
                if (optional != null) {
                    consolidated.add(optional);
                }
                consolidated.add(new ZeroOrMoreNode());
                consolidated.add(new AnyCharNode());
            } else {
                // Otherwise reset the index to the initial index.
                iter.setIndex(lastIndex);
            }
        }
        
        // Iterate through each child.
        while (iter.hasNext()) {
            int lastIndex = iter.index();
            iter.seekPastOptional();
            iter.seekPastQuantifier();
            
            // Do not call next until we know the next node can match zero.
            Node next = iter.peekNext();
            // The next node can match zero. Call next, and call the specific consolidation method based on whether the node can match only zero, or other
            // numbers.
            if (RegexUtils.matchesZero(next)) {
                if (RegexUtils.matchesZeroOnly(next)) {
                    iter.setIndex(lastIndex);
                    consolidated.addAll(consolidateTrailingMatchesZeroOnly(iter));
                } else {
                    iter.setIndex(lastIndex);
                    consolidated.addAll(consolidateTrailingMatchesZero(iter));
                }
            } else {
                // Reset the index to the non-zero matching element.
                iter.setIndex(lastIndex);
                break;
            }
        }
        
        // Add the remaining nodes to the list to return.
        while (iter.hasNext()) {
            consolidated.add(iter.next());
        }
        return consolidated;
    }
    
    /**
     * Consolidate any trailing zeros that can possibly match zero.
     * 
     * @param iter
     *            the iterator
     * @return the consolidated nodes.
     */
    private List<Node> consolidateTrailingMatchesZero(NodeListIterator iter) {
        List<Node> nodes = new ArrayList<>();
        while (iter.hasNext()) {
            int lastIndex = iter.index();
            
            // Skip past and capture the optional and quantifier for the node if present.
            Node optional = iter.isNextOptional() ? iter.next() : null;
            Node quantifier = iter.isNextQuantifier() ? iter.next() : null;
            
            // Do not call next until we know the next node can match zero.
            Node next = iter.peekNext();
            // The next node can match zero. The first call to next should always return an element that can match zero, but not only zero.
            if (RegexUtils.matchesZero(next)) {
                iter.next();
                
                if (quantifier != null) {
                    // If the next node had a quantifier, retain them, and any optionals that were present.
                    if (optional != null) {
                        nodes.add(optional);
                    }
                    nodes.add(quantifier);
                    nodes.add(next);
                } else {
                    // This is a single element. Make it optional.
                    nodes.add(new OptionalNode());
                    // If the next node did not have a quantifier, check if the current node is a character class. If so, remove the possibility for the
                    // character class to match against zero.
                    if (next instanceof CharClassNode) {
                        nodes.add(removeZeroFromCharClass(next));
                    } else {
                        nodes.add(next);
                    }
                }
                
                // If there are any elements after the current element that only match zero, consolidate then and add the result.
                if (iter.hasNext()) {
                    lastIndex = iter.index();
                    iter.seekPastOptional();
                    iter.seekPastQuantifier();
                    if (RegexUtils.matchesZeroOnly(iter.peekNext())) {
                        iter.setIndex(lastIndex);
                        nodes.addAll(consolidateTrailingMatchesZeroOnly(iter));
                    } else {
                        iter.setIndex(lastIndex);
                    }
                }
            } else {
                // The next element cannot match zero. Nothing more to do.
                iter.setIndex(lastIndex);
                break;
            }
        }
        return nodes;
    }
    
    /**
     * Consolidate the next consecutive elements that can only match zero.
     * 
     * @param iter
     *            the iterator
     * @return a list of the consolidated nodes
     */
    private List<Node> consolidateTrailingMatchesZeroOnly(NodeListIterator iter) {
        // We need to track the minimum and maximum times a leading zero can occur.
        int minZeroCount = 0;
        int maxZeroCount = 0;
        
        while (iter.hasNext()) {
            int lastIndex = iter.index();
            // Skip any optional if present.
            iter.seekPastOptional();
            // Grab the quantifier if present.
            Node quantifier = iter.isNextQuantifier() ? iter.next() : null;
            
            // Do not call next until we've confirmed the next node only matches zero.
            Node next = iter.peekNext();
            if (RegexUtils.matchesZeroOnly(next)) {
                // Explicitly call next now.
                iter.next();
                // If the zero has a quantifier, extract the quantifier range.
                if (quantifier != null) {
                    Pair<Integer,Integer> quantifierRange = RegexUtils.getQuantifierRange(quantifier);
                    // Increment the lower bound.
                    minZeroCount += quantifierRange.getLeft();
                    if (maxZeroCount != -1) {
                        // If the quantifier range has no defined upper bound, that is equivalent to unlimited. Set the max bound to -1 to ensure it is not
                        // changed.
                        if (quantifierRange.getRight() == null) {
                            maxZeroCount = -1;
                        } else {
                            // Otherwise increment the upper bound.
                            maxZeroCount += quantifierRange.getRight();
                        }
                    }
                } else {
                    // The zero does not have a quantifier. Increment the min count by one, and increment the max count only if we have not yet determined that
                    // the max should be considered unlimited.
                    minZeroCount++;
                    if (maxZeroCount != -1) {
                        maxZeroCount++;
                    }
                }
            } else {
                // If the next node does not only match zero, stop iterating.
                iter.setIndex(lastIndex);
                break;
            }
        }
        
        List<Node> nodes = new ArrayList<>();
        // The new element must be optional. Since the nodes are in reverse order, add the optional first.
        nodes.add(new OptionalNode());
        
        // If the min and max are both 1, return 0?
        if (minZeroCount == 1 && maxZeroCount == 1) {
            nodes.add(new SingleCharNode(RegexConstants.ZERO));
        } else {
            // Otherwise we need return 0 followed by a quantifier inside an optional group.
            GroupNode groupNode = new GroupNode();
            groupNode.addChild(new SingleCharNode(RegexConstants.ZERO));
            
            if (maxZeroCount == -1 && minZeroCount < 2) {
                if (minZeroCount == 0) {
                    // Return (0*)?
                    groupNode.addChild(new ZeroOrMoreNode());
                } else if (minZeroCount == 1) {
                    // Return (0+)?
                    groupNode.addChild(new OneOrMoreNode());
                }
            } else {
                RepetitionNode repetition = new RepetitionNode();
                if (minZeroCount == maxZeroCount) {
                    // Return (0{x})?
                    IntegerNode integer = new IntegerNode(minZeroCount);
                    repetition.addChild(integer);
                } else {
                    // Return (0{x,y})? or (0{x,})? if unlimited max.
                    IntegerRangeNode integerRange = new IntegerRangeNode();
                    integerRange.setStart(minZeroCount);
                    if (maxZeroCount != -1) {
                        integerRange.setEnd(maxZeroCount);
                    }
                    repetition.addChild(integerRange);
                }
                
                groupNode.addChild(repetition);
            }
            
            nodes.add(groupNode);
        }
        
        return nodes;
    }
    
    /**
     * Remove the ability for the character class to match the character '0'.
     * 
     * @param node
     *            the character class node
     * @return a new character class that cannot match '0'.
     */
    private Node removeZeroFromCharClass(Node node) {
        CharClassNode charClass = new CharClassNode();
        if (charClass.isNegated()) {
            // If the character class is negated, simply add a zero to it.
            charClass.addChild(new SingleCharNode(RegexConstants.ZERO));
        } else {
            // If the character class is not negated, remove 0 from it.
            for (Node child : node.getChildren()) {
                if (child instanceof SingleCharNode) {
                    // Only retain single characters that are not zero.
                    if (!RegexUtils.isChar(child, RegexConstants.ZERO)) {
                        charClass.addChild(child);
                    }
                } else {
                    // If the range starts at zero, make the range start at one.
                    CharRangeNode range = (CharRangeNode) child;
                    if (range.getStart() == RegexConstants.ZERO) {
                        if (range.getEnd() != RegexConstants.ZERO) {
                            range.setStart(RegexConstants.ONE);
                            charClass.addChild(range);
                        }
                    } else {
                        charClass.addChild(range);
                    }
                }
            }
        }
        return charClass;
    }
}