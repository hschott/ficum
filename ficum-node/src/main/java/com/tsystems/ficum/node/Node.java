package com.tsystems.ficum.node;

public interface Node {

    void accept(Visitor<?> visitor);

}
