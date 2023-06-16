package datawave.data.normalizer.regex;

import java.util.Objects;

/**
 * Represents a character class in a regex pattern encapsulated by {@code [...]}.
 */
public class CharClassNode extends Node {
    
    private boolean negated;
    
    public CharClassNode() {}
    
    public CharClassNode(boolean negated) {
        this.negated = negated;
    }
    
    public boolean isNegated() {
        return negated;
    }
    
    public void negate() {
        this.negated = true;
    }
    
    @Override
    public NodeType getType() {
        return NodeType.CHAR_CLASS;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitCharClass(this, data);
    }
    
    @Override
    public CharClassNode shallowCopy() {
        return new CharClassNode(this.negated);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CharClassNode that = (CharClassNode) o;
        return negated == that.negated;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(negated);
    }
}
