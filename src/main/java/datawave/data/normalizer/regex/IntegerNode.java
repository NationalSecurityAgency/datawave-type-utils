package datawave.data.normalizer.regex;

import datawave.data.normalizer.regex.visitor.Visitor;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents an integer parsed in a regex repetition that did not contain a range, e.g. {@code {3}}.
 */
public class IntegerNode extends Node {
    
    private int value;
    
    public IntegerNode(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    @Override
    public NodeType getType() {
        return NodeType.INTEGER;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitInteger(this, data);
    }
    
    @Override
    public IntegerNode shallowCopy() {
        return new IntegerNode(this.value);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IntegerNode that = (IntegerNode) o;
        return value == that.value;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return new StringJoiner(", ", IntegerNode.class.getSimpleName() + "[", "]").add("value=" + value).toString();
    }
}
