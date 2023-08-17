package datawave.data.normalizer.regex;

import datawave.data.normalizer.regex.visitor.Visitor;

import java.util.Collection;

/**
 * Represents an encoded simple number in a regex tree.
 */
public class EncodedNumberNode extends Node {
    
    public EncodedNumberNode() {}
    
    public EncodedNumberNode(Collection<Node> children) {
        addChildren(children);
    }
    
    @Override
    public NodeType getType() {
        return NodeType.ENCODED_NUMBER;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitEncodedNumber(this, data);
    }
    
    @Override
    public Node shallowCopy() {
        return new EncodedNumberNode();
    }
}
