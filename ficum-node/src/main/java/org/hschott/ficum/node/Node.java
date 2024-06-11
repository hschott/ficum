package org.hschott.ficum.node;

public interface Node {

    void accept(Visitor<?> visitor);

}
