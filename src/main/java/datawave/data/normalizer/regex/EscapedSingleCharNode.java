package datawave.data.normalizer.regex;

import datawave.data.normalizer.regex.visitor.Visitor;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents an escaped character in a regex pattern, e.g. {@code \-}.
 */
public class EscapedSingleCharNode extends Node {
    
    private char character;
    
    public EscapedSingleCharNode(char character) {
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
        return NodeType.ESCAPED_SINGLE_CHAR;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitEscapedSingleChar(this, data);
    }
    
    @Override
    public EscapedSingleCharNode shallowCopy() {
        return new EscapedSingleCharNode(this.character);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EscapedSingleCharNode that = (EscapedSingleCharNode) o;
        return character == that.character;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(character);
    }
    
    @Override
    public String toString() {
        return new StringJoiner(", ", EscapedSingleCharNode.class.getSimpleName() + "[", "]").add("character=" + character).toString();
    }
}
