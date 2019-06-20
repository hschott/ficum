package de.bitgrip.ficum.node;

public class ConstraintNode<T> implements OperandNode {

    private Constraint<T> constraint;

    public ConstraintNode(Constraint<T> constraint) {
        super();
        this.constraint = constraint;
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
        if (!(obj instanceof ConstraintNode))
            return false;
        ConstraintNode<?> other = (ConstraintNode<?>) obj;
        if (constraint == null) {
            if (other.constraint != null)
                return false;
        } else if (!constraint.equals(other.constraint))
            return false;
        return true;
    }

    public T getArgument() {
        return constraint != null ? constraint.getArgument() : null;
    }

    public Comparison getComparison() {
        return constraint != null ? constraint.getComparison() : null;
    }

    public String getSelector() {
        return constraint != null ? constraint.getSelector() : null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((constraint == null) ? 0 : constraint.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s", constraint);
    }

}
