package org.ficum.node;

public interface Visitor<T> {

    T start(Node root);

    void visit(AndNode node);

    void visit(ConstraintNode node);

    void visit(Node node);

    void visit(OrNode node);
}
