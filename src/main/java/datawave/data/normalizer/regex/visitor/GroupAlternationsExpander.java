package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.AlternationNode;
import datawave.data.normalizer.regex.ExpressionNode;
import datawave.data.normalizer.regex.GroupNode;
import datawave.data.normalizer.regex.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link CopyVisitor} that will return a copy of the tree where all {@link GroupNode} instances that have alternation children are expanded
 * to move the sub expressions of each group alternations up to a top-level regex alternations so that no groups contain any alternations. See the following
 * examples:
 * <ul>
 * <li>Input {@code "(42|54).*"} will return {@code "(42).*|(54).*"}</li>
 * <li>Input {@code "(3|4)(5|6)"} will return {@code "(3)(5)|(3)(6)|(4)(5)|(4)(6)"}</li>
 * <li>Input {@code "[45](45|99.*)|.+?(543){3}"} will return {@code "[45](45)|[45](99.*)|.+?(543){3}"}</li>
 * </ul>
 */
public class GroupAlternationsExpander extends SubExpressionVisitor {
    
    /**
     * Return a copy of the given tree with all group alternations expanded.
     * 
     * @param node
     *            the node to expand
     * @return the expanded node
     */
    public static Node expand(Node node) {
        if (node == null) {
            return null;
        }
        GroupAlternationsExpander visitor = new GroupAlternationsExpander();
        return (Node) node.accept(visitor, null);
    }
    
    @Override
    protected Object visitSubExpression(Node node) {
        // If the node has any group children, visit each child and retain any expansions.
        if (node.isAnyChildOf(GroupNode.class)) {
            // The list of sub-expressions.
            List<Node> expressions = new ArrayList<>();
            expressions.add(new ExpressionNode());
            
            // Visit each child.
            for (Node child : node.getChildren()) {
                Node visited = (Node) child.accept(this, null);
                if (visited instanceof AlternationNode) {
                    // If an alternation was returned, expansion occurred. Create a new set of expression nodes that effectively represent a cartesian product
                    // between the existing list of sub-expressions, and the alternation's children.
                    List<Node> groups = visited.getChildren();
                    List<Node> newExpressions = new ArrayList<>();
                    for (Node expression : expressions) {
                        for (Node group : groups) {
                            Node copy = copy(expression);
                            copy.addChild(group);
                            newExpressions.add(copy);
                        }
                    }
                    // Replace the old sub-expressions with the new ones.
                    expressions = newExpressions;
                } else {
                    // If the node was not an alternation, expansion did not occur for this node. Add it as a child to each expression.
                    expressions.forEach((exp) -> exp.addChild(visited));
                }
            }
            
            // If no expansion occurred, return the single expression.
            if (expressions.size() == 1) {
                return expressions.get(0);
            } else {
                // Otherwise, return an alternation with each expanded expression, wrapped in its own expression node.
                AlternationNode alternation = new AlternationNode();
                for (Node expression : expressions) {
                    if (expression.getChildCount() == 1 && expression.getFirstChild() instanceof GroupNode) {
                        alternation.addChild(expression.getFirstChild());
                    } else {
                        alternation.addChild(expression);
                    }
                }
                return new ExpressionNode(alternation);
            }
        } else {
            // Otherwise return a simple copy.
            return copy(node);
        }
    }
    
    @Override
    public Object visitGroup(GroupNode node, Object data) {
        if (node.getFirstChild() instanceof AlternationNode) {
            List<GroupNode> groups = new ArrayList<>();
            for (Node child : node.getFirstChild().getChildren()) {
                groups.add(new GroupNode(child));
            }
            return new AlternationNode(groups);
        } else {
            return copy(node);
        }
    }
}
