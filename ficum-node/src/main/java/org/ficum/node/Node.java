package org.ficum.node;

public interface Node {

    void accept(Visitor<?> visitor);

}
