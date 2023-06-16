package datawave.data.normalizer.regex;

/**
 * Represents the digit character class {@code \d} in a regex pattern.
 */
public class DigitCharClassNode extends Node {
    
    @Override
    public NodeType getType() {
        return NodeType.DIGIT_CHAR_CLASS;
    }
    
    @Override
    public Object accept(Visitor visitor, Object data) {
        return visitor.visitDigitChar(this, data);
    }
    
    @Override
    public DigitCharClassNode shallowCopy() {
        return new DigitCharClassNode();
    }
}
