package org.hschott.ficum.node;

public class LogicalOperationNode implements OperationNode {

    private Node left;

    private Node right;

    private final Operator operator;

    public LogicalOperationNode(Operator operator) {
        super();
        this.operator = operator;
    }

    public void accept(Visitor<?> visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof LogicalOperationNode))
            return false;

        LogicalOperationNode other = (LogicalOperationNode) obj;
        if (left == null) {
            if (other.left != null)
                return false;
        } else if (!left.equals(other.left))
            return false;

        if (right == null) {
            if (other.right != null)
                return false;
        } else if (!right.equals(other.right))
            return false;

        return getOperator().equals(other.getOperator());

    }

    public Node getLeft() {
        return left;
    }

    public Operator getOperator() {
        return operator;
    }

    public Node getRight() {
        return right;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((left == null) ? 0 : left.hashCode());
        result = prime * result + ((right == null) ? 0 : right.hashCode());
        result = prime * result + getOperator().hashCode();
        return result;
    }

    public OperationNode setLeft(Node left) {
        this.left = left;
        return this;
    }

    public OperationNode setRight(Node right) {
        this.right = right;
        return this;
    }

    @Override
    public String toString() {
        return String.format("(%s %s %s)", left, getOperator(), right);
    }

}
