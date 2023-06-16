package datawave.data.normalizer.regex;

/**
 * Represents the star in a regex pattern.
 */
public class ZeroOrMoreNode extends Node {
    
    @Override
    public NodeType getType() {
        return NodeType.ZERO_OR_MORE;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitZeroToMany(this, data);
    }
    
    @Override
    public ZeroOrMoreNode shallowCopy() {
        return new ZeroOrMoreNode();
    }
}
