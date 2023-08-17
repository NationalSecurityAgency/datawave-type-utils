package datawave.data.normalizer.regex;

import datawave.data.normalizer.regex.visitor.Visitor;

import java.util.Collection;

/**
 * Represents an encoded pattern in a regex tree.
 */
public class EncodedPatternNode extends Node {
    
    public EncodedPatternNode() {}
    
    public EncodedPatternNode(Node child) {
        super(child);
    }
    
    public EncodedPatternNode(Collection<? extends Node> children) {
        super(children);
    }
    
    @Override
    public NodeType getType() {
        return NodeType.ENCODED_PATTERN;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitEncodedPattern(this, data);
    }
    
    @Override
    public Node shallowCopy() {
        return new EncodedPatternNode();
    }
}
