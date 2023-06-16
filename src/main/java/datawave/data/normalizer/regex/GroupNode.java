package datawave.data.normalizer.regex;

import java.util.List;

/**
 * Represents a regex group in a regex pattern encapsulated by {@code (...)}.
 */
public class GroupNode extends Node {
    
    public GroupNode() {}
    
    public GroupNode(Node child) {
        super(child);
    }
    
    public GroupNode(List<? extends Node> children) {
        super(children);
    }
    
    @Override
    public NodeType getType() {
        return NodeType.GROUP;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitGroup(this, data);
    }
    
    @Override
    public GroupNode shallowCopy() {
        return new GroupNode();
    }
}
