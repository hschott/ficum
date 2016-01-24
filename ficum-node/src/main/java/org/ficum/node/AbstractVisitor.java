package org.ficum.node;

public abstract class AbstractVisitor<T> implements Visitor<T> {

    private boolean alwaysWildcard = false;

    public static boolean containsWildcard(String value) {
        return value.contains("*") || value.contains("?");
    }

    public boolean isAlwaysWildcard() {
        return alwaysWildcard;
    }

    public void setAlwaysWildcard(boolean alwaysWildcardMatch) {
        this.alwaysWildcard = alwaysWildcardMatch;
    }

    public void visit(Node node) {
        if (node instanceof ConstraintNode) {
            visit((ConstraintNode<?>) node);
            return;
        }
        if (node instanceof OperationNode) {
            visit((OperationNode) node);
            return;
        }
    }

}
