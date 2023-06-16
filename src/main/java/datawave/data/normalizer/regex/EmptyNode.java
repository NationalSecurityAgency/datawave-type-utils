package datawave.data.normalizer.regex;

/**
 * Placeholder empty node for empty groups or empty alternation branches.
 */
public class EmptyNode extends Node {
    
    @Override
    public NodeType getType() {
        return NodeType.EMPTY;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitEmpty(this, data);
    }
    
    @Override
    public Node shallowCopy() {
        return new EmptyNode();
    }
}
