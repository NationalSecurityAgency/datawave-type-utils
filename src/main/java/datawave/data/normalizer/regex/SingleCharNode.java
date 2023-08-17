package datawave.data.normalizer.regex;

import datawave.data.normalizer.regex.visitor.Visitor;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a single, non-special character in a regex pattern.
 */
public class SingleCharNode extends Node {
    
    private char character;
    
    public SingleCharNode(char character) {
        this.character = character;
    }
    
    public char getCharacter() {
        return character;
    }
    
    public void setCharacter(char character) {
        this.character = character;
    }
    
    @Override
    public NodeType getType() {
        return NodeType.SINGLE_CHAR;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitSingleChar(this, data);
    }
    
    @Override
    public SingleCharNode shallowCopy() {
        return new SingleCharNode(this.character);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SingleCharNode that = (SingleCharNode) o;
        return character == that.character;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(character);
    }
    
    @Override
    public String toString() {
        return new StringJoiner(", ", SingleCharNode.class.getSimpleName() + "[", "]").add("character=" + character).toString();
    }
}
