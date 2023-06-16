package datawave.data.normalizer.regex.visitor;

import datawave.data.normalizer.regex.GroupNode;
import datawave.data.normalizer.regex.Node;

/**
 * Implementation of {@link BaseVisitor} that accepts a {@link Node} and verifies that there are no nested groups.
 */
public class NestedGroupValidator extends BaseVisitor {
    
    public static void validate(Node node) {
        if (node != null) {
            NestedGroupValidator visitor = new NestedGroupValidator();
            node.accept(visitor, false);
        }
    }
    
    @Override
    public Object visitGroup(GroupNode node, Object data) {
        // If data is true, this group node was found nested in another group node.
        if (((boolean) data)) {
            throw new IllegalArgumentException("Nested groups are not supported.");
        } else {
            // Otherwise, this is a top-level group node. Examine the children for any nested groups.
            for (Node child : node.getChildren()) {
                child.accept(this, true);
            }
        }
        return null;
    }
}
