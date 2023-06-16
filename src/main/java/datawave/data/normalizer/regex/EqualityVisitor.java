package datawave.data.normalizer.regex;

/**
 * A {@link Visitor} implementation that will compare two {@link Node} tree and determine if they are equal.
 */
public class EqualityVisitor implements Visitor {
    
    /**
     * Return whether the given {@link Node} trees are equal.
     * 
     * @param left
     *            the left tree to compare
     * @param right
     *            the right tree to compare
     * @return true if the trees are equal, or false otherwise.
     */
    public static boolean isEqual(Node left, Node right) {
        if (left != null && right != null) {
            EqualityVisitor visitor = new EqualityVisitor();
            return (boolean) left.accept(visitor, right);
        } else {
            return left == null && right == null;
        }
    }
    
    private boolean isEqual(Node left, Object data) {
        Node right = (Node) data;
        // Compare the nodes.
        if (!left.equals(right)) {
            return false;
        }
        // Compare the child counts.
        if (left.getChildCount() != right.getChildCount()) {
            return false;
        }
        // Compare the children.
        for (int index = 0; index < left.getChildCount(); index++) {
            Node leftChild = left.getChildAt(index);
            Node rightChild = right.getChildAt(index);
            boolean isEqual = (boolean) leftChild.accept(this, rightChild);
            if (!isEqual) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public Object visitExpression(ExpressionNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitAlternation(AlternationNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitGroup(GroupNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitDigitChar(DigitCharClassNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitCharClass(CharClassNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitCharRange(CharRangeNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitSingleChar(SingleCharNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitEscapedSingleChar(EscapedSingleCharNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitRepetition(RepetitionNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitOptional(OptionalNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitAnyChar(AnyCharNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitZeroToMany(ZeroOrMoreNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitOneToMany(OneOrMoreNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitInteger(IntegerNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitIntegerRange(IntegerRangeNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitEmpty(EmptyNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitStartAnchor(StartAnchorNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitEndAnchor(EndAnchorNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitEncodedNumber(EncodedNumberNode node, Object data) {
        return isEqual(node, data);
    }
    
    @Override
    public Object visitEncodedPattern(EncodedPatternNode node, Object data) {
        return isEqual(node, data);
    }
}
