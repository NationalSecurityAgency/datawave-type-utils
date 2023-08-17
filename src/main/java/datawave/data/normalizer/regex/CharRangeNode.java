package datawave.data.normalizer.regex;

import datawave.data.normalizer.regex.visitor.Visitor;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents a character range defined in a character class in a regex pattern.
 */
public class CharRangeNode extends Node {
    
    private char start;
    private char end;
    
    public CharRangeNode(char start, char end) {
        this.start = start;
        this.end = end;
    }
    
    public char getStart() {
        return start;
    }
    
    public void setStart(char start) {
        this.start = start;
    }
    
    public char getEnd() {
        return end;
    }
    
    public void setEnd(char end) {
        this.end = end;
    }
    
    @Override
    public NodeType getType() {
        return NodeType.CHAR_RANGE;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitCharRange(this, data);
    }
    
    @Override
    public CharRangeNode shallowCopy() {
        return new CharRangeNode(this.start, this.end);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CharRangeNode that = (CharRangeNode) o;
        return start == that.start && end == that.end;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
    
    @Override
    public String toString() {
        return new StringJoiner(", ", CharRangeNode.class.getSimpleName() + "[", "]").add("start=" + start).add("end=" + end).toString();
    }
}
