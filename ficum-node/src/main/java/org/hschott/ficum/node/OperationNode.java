package org.hschott.ficum.node;

public interface OperationNode extends Node {

    Node getLeft();

    Operator getOperator();

    Node getRight();

    void setLeft(Node node);

    void setRight(Node node);

}
