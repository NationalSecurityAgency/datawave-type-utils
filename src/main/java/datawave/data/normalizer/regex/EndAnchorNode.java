package datawave.data.normalizer.regex;

/**
 * Represents a regex end anchor, i.e. {@code $}.
 */
public class EndAnchorNode extends Node {
    
    @Override
    public NodeType getType() {
        return NodeType.END_ANCHOR;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitEndAnchor(this, data);
    }
    
    @Override
    public Node shallowCopy() {
        return new EndAnchorNode();
    }
}
