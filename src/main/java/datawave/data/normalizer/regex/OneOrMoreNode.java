package datawave.data.normalizer.regex;

/**
 * Represents the plus sign in a regex pattern.
 */
public class OneOrMoreNode extends Node {
    
    @Override
    public NodeType getType() {
        return NodeType.ONE_OR_MORE;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitOneToMany(this, data);
    }
    
    @Override
    public OneOrMoreNode shallowCopy() {
        return new OneOrMoreNode();
    }
}
