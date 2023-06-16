package datawave.data.normalizer.regex;

public interface Visitor {
    
    Object visitExpression(ExpressionNode node, Object data);
    
    Object visitAlternation(AlternationNode node, Object data);
    
    Object visitGroup(GroupNode node, Object data);
    
    Object visitDigitChar(DigitCharClassNode node, Object data);
    
    Object visitCharClass(CharClassNode node, Object data);
    
    Object visitCharRange(CharRangeNode node, Object data);
    
    Object visitSingleChar(SingleCharNode node, Object data);
    
    Object visitEscapedSingleChar(EscapedSingleCharNode node, Object data);
    
    Object visitRepetition(RepetitionNode node, Object data);
    
    Object visitOptional(OptionalNode node, Object data);
    
    Object visitAnyChar(AnyCharNode node, Object data);
    
    Object visitZeroToMany(ZeroOrMoreNode node, Object data);
    
    Object visitOneToMany(OneOrMoreNode node, Object data);
    
    Object visitInteger(IntegerNode node, Object data);
    
    Object visitIntegerRange(IntegerRangeNode node, Object data);
    
    Object visitEmpty(EmptyNode node, Object data);
    
    Object visitStartAnchor(StartAnchorNode node, Object data);
    
    Object visitEndAnchor(EndAnchorNode node, Object data);
    
    Object visitEncodedNumber(EncodedNumberNode node, Object data);
    
    Object visitEncodedPattern(EncodedPatternNode node, Object data);
}
