package datawave.data.normalizer.regex;

import datawave.data.normalizer.regex.visitor.Visitor;

import java.util.Map;

/**
 * Represents the question mark in a regex pattern.
 */
public class QuestionMarkNode extends Node {
    
    public QuestionMarkNode() {}
    
    public QuestionMarkNode(Map<String,String> properties) {
        super(properties);
    }
    
    @Override
    public NodeType getType() {
        return NodeType.OPTIONAL;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitQuestionMark(this, data);
    }
    
    @Override
    public QuestionMarkNode shallowCopy() {
        return new QuestionMarkNode(this.properties);
    }
}
