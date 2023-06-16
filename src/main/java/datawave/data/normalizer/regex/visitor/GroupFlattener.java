package datawave.data.normalizer.regex.visitor;

import com.google.common.collect.ImmutableSet;
import datawave.data.normalizer.regex.AlternationNode;
import datawave.data.normalizer.regex.ExpressionNode;
import datawave.data.normalizer.regex.GroupNode;
import datawave.data.normalizer.regex.Node;
import datawave.data.normalizer.regex.OneOrMoreNode;
import datawave.data.normalizer.regex.OptionalNode;
import datawave.data.normalizer.regex.RepetitionNode;
import datawave.data.normalizer.regex.ZeroOrMoreNode;

import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link CopyVisitor} that will return a copy of the tree with all groups flattened where possible. It is expected for this visitor to be
 * used on a tree that has had its groups expanded via {@link GroupAlternationsExpander#expand(Node)}, and as such, should not be used on any trees that have
 * group nodes with child alternations. See the following examples:
 * <ul>
 * <li>Input {@code "(432).*|(65.*)} will return {@code "432.*|65.*"}</li>
 * <li>Input {@code "(43[3-6])54"} will return {@code "43[3-6]54"}</li>
 * <li>Input {@code "(3)(4)|(5).*(76)"} will return {@code "34|5.*76"}</li>
 * </ul>
 * Groups immediately followed by {@code * + ?} or a repetition will not be flattened if they contain expressions that are not:
 * <ol>
 * <li>One character long, e.g. {@code "(.)"}, {@code "(3)"}, {@code "(\d)"}, {@code "(\.)"}</li>
 * <li>An expression that consists solely of a character class list, e.g. {@code "([3-4])"}</li>
 * </ol>
 * See the following examples of groups involving these special cases..
 * <ul>
 * <li>Input {@code "(234)*"} will return {@code "(234)*"}</li>
 * <li>Input {@code "(234)+"} will return {@code "(234)+"}</li>
 * <li>Input {@code "(234)?"} will return {@code "(234)?"}</li>
 * <li>Input {@code "(234){3}"} will return {@code "(234){3}"}</li>
 * <li>Input {@code "(2)*"} will return {@code "2*"}</li>
 * <li>Input {@code "(.)+"} will return {@code ".+"}</li>
 * <li>Input {@code "(\d)?"} will return {@code "\d?"}</li>
 * <li>Input {@code "([3-4]){3}"} will return {@code "[3-4]{3}"}</li>
 * </ul>
 * 
 * @see GroupAlternationsExpander
 */
public class GroupFlattener extends CopyVisitor {
    
    private static final Set<Class<? extends Node>> TYPES_PROHIBITING_FLATTENING = ImmutableSet.of(ZeroOrMoreNode.class, OneOrMoreNode.class,
                    OptionalNode.class, RepetitionNode.class);
    
    /**
     * Return a copy of the given tree with groups flattened where possible.
     * 
     * @param node
     *            the node to flatten
     * @return the flattened tree
     */
    public static Node flatten(Node node) {
        if (node == null) {
            return null;
        }
        GroupFlattener visitor = new GroupFlattener();
        return (Node) node.accept(visitor, null);
    }
    
    @Override
    public Object visitExpression(ExpressionNode node, Object data) {
        // If there are any group node children, attempt to flatten it.
        if (node.isAnyChildOf(GroupNode.class)) {
            ExpressionNode newNode = new ExpressionNode();
            List<Node> children = node.getChildren();
            int lastIndex = children.size() - 1;
            for (int index = 0; index < children.size(); index++) {
                Node current = children.get(index);
                if (current instanceof GroupNode) {
                    // If we have another node to the right of this node, check if it will prohibit flattening.
                    if (index < lastIndex) {
                        Node next = children.get((index + 1));
                        // Check if we have a type that prohibits flattening.
                        if (TYPES_PROHIBITING_FLATTENING.contains(next.getClass())) {
                            // We can still flatten the child if it only has one grandchild.
                            if (current.getFirstChild().getChildCount() == 1) {
                                current.getFirstChild().getChildren().stream().map(CopyVisitor::copy).forEach(newNode::addChild);
                            } else {
                                // Otherwise add a copy of the group node.
                                newNode.addChild(copy(current));
                            }
                        } else {
                            // Otherwise add copies of the group node's grandchildren to the new node.
                            current.getFirstChild().getChildren().stream().map(CopyVisitor::copy).forEach(newNode::addChild);
                        }
                    } else {
                        // Otherwise add copies of the group node's grandchildren to the new node.
                        current.getFirstChild().getChildren().stream().map(CopyVisitor::copy).forEach(newNode::addChild);
                    }
                } else {
                    newNode.addChild(copy(current));
                }
            }
            return newNode;
        } else {
            return super.visitExpression(node, data);
        }
    }
    
    @Override
    public Object visitAlternation(AlternationNode node, Object data) {
        AlternationNode newNode = new AlternationNode();
        for (Node child : node.getChildren()) {
            if (child instanceof ExpressionNode) {
                // Add the flattened child.
                newNode.addChild((Node) child.accept(this, data));
            } else if (child instanceof GroupNode) {
                // Group nodes should not have alternations at this point, it's expected for group alternations to have been expanded. Because of this, the
                // group should only have a single expression node child. Add a copy of the expression node to the new node.
                newNode.addChild(copy(child.getFirstChild()));
            } else {
                // We should never see any other types as children of alternation nodes. If we do, something went wrong somewhere.
                throw new IllegalStateException("Encountered unexpected child of alternation node of type " + child.getClass().getSimpleName());
            }
        }
        return newNode;
    }
}
