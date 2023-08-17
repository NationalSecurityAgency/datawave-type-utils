package datawave.data.normalizer.regex;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Iterator implementation that can traverse back and forth over a list of nodes.
 */
public class NodeListIterator {
    
    /**
     * The list.
     */
    private final List<Node> nodes;
    
    /**
     * The current index.
     */
    private int index;
    
    public NodeListIterator(List<Node> nodes) {
        this.nodes = nodes;
    }
    
    /**
     * Return the current iterator index.
     * 
     * @return the index
     */
    public int index() {
        return index;
    }
    
    /**
     * Set the current index for the iterator.
     * 
     * @param index
     *            the index
     */
    public void setIndex(int index) {
        this.index = index;
    }
    
    /**
     * Return true if there are more nodes to return from the list.
     * 
     * @return true if there are a next node to return
     */
    public boolean hasNext() {
        return this.index < this.nodes.size();
    }
    
    /**
     * Return the next node from the list.
     * 
     * @return the next node
     * @throws NoSuchElementException
     *             if there is no next node
     */
    public Node next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return nodes.get(index++);
    }
    
    /**
     * Return the next node from the list without modifying the current iterator index.
     * 
     * @return the next node
     * @throws NoSuchElementException
     *             if there is no next node
     */
    public Node peekNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return nodes.get((index));
    }
    
    /**
     * Return whether the next node is an instance of the given type.
     * 
     * @param type
     *            the type
     * @return true if the next node is an instance of the type, or false otherwise
     * @throws NoSuchElementException
     *             if there is no next node
     */
    public boolean isNextInstanceOf(Class<? extends Node> type) {
        return type.isInstance(peekNext());
    }
    
    /**
     * Return whether the next node is an instance of one of the given types.
     * 
     * @param types
     *            the types
     * @return true if the next node is an instance of one of the given types, or false otherwise
     * @throws NoSuchElementException
     *             if there is no next node
     */
    public boolean isNextInstanceOfAny(Collection<Class<? extends Node>> types) {
        Node previous = peekNext();
        return types.stream().anyMatch((type) -> type.isInstance(previous));
    }
    
    public void seekPastZeroMatchingElements() {
        while (hasNext()) {
            // Peek at the next node.
            Node next = peekNext();
            // We have a leading zero. Skip it.
            if (RegexUtils.matchesZero(next)) {
                // Explicitly call next so that we increment the iterator index.
                next();
                // Seek past a succeeding quantifier and optional if present.
                seekPastQuantifier();
                seekPastOptional();
            } else {
                return;
            }
        }
    }
    
    public void seekPastZeroOnlyElements() {
        while (hasNext()) {
            // Peek at the next node.
            Node next = peekNext();
            // We have a leading zero. Skip it.
            if (RegexUtils.matchesZeroOnly(next)) {
                // Explicitly call next so that we increment the iterator index.
                next();
                // Seek past a succeeding quantifier and optional if present.
                seekPastQuantifier();
                seekPastOptional();
            } else {
                return;
            }
        }
    }
    
    public void seekPastQuantifier() {
        if (hasNext() && isNextInstanceOfAny(RegexConstants.QUANTIFIER_TYPES)) {
            next();
        }
    }
    
    public void seekPastOptional() {
        if (hasNext() && isNextInstanceOf(OptionalNode.class)) {
            next();
        }
    }
    
    public boolean isNextQuantifier() {
        return hasNext() && isNextInstanceOfAny(RegexConstants.QUANTIFIER_TYPES);
    }
    
    public boolean isNextOptional() {
        return hasNext() && isNextInstanceOf(OptionalNode.class);
    }
}
