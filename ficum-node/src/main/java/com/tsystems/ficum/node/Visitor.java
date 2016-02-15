package com.tsystems.ficum.node;

public interface Visitor<T> {

    T start(Node root);

    void visit(ConstraintNode<?> node);

    void visit(Node node);

    void visit(OperationNode node);

}
