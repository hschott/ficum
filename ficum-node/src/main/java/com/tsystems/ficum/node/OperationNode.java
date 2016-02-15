package com.tsystems.ficum.node;

public interface OperationNode extends Node {

    Node getLeft();

    Operator getOperator();

    Node getRight();

    OperationNode setLeft(Node node);

    OperationNode setRight(Node node);

}
