package datawave.data.normalizer.regex;

import datawave.data.normalizer.regex.visitor.Visitor;

/**
 * Represents the question mark in a regex pattern.
 */
public class QuestionMarkNode extends Node {
    
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
        return new QuestionMarkNode();
    }
}
