package datawave.data.normalizer.regex;

/**
 * Represents the question mark in a regex pattern.
 */
public class OptionalNode extends Node {
    
    @Override
    public NodeType getType() {
        return NodeType.OPTIONAL;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitOptional(this, data);
    }
    
    @Override
    public OptionalNode shallowCopy() {
        return new OptionalNode();
    }
}
