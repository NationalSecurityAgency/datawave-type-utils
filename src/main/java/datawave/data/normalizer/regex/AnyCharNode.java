package datawave.data.normalizer.regex;

/**
 * Represents a dot in a regex pattern.
 */
public class AnyCharNode extends Node {
    
    @Override
    public NodeType getType() {
        return NodeType.ANY_CHAR;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitAnyChar(this, data);
    }
    
    @Override
    public AnyCharNode shallowCopy() {
        return new AnyCharNode();
    }
}
