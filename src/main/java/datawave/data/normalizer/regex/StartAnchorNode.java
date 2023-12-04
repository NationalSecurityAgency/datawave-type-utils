package datawave.data.normalizer.regex;

import datawave.data.normalizer.regex.visitor.Visitor;

import java.util.Map;

/**
 * Represents a regex start anchor, i.e. {@code ^}.
 */
public class StartAnchorNode extends Node {
    
    protected StartAnchorNode() {
        super();
    }
    
    public StartAnchorNode(Map<String,String> properties) {
        super(properties);
    }
    
    @Override
    public NodeType getType() {
        return NodeType.START_ANCHOR;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitStartAnchor(this, data);
    }
    
    @Override
    public Node shallowCopy() {
        return new StartAnchorNode(this.properties);
    }
}
