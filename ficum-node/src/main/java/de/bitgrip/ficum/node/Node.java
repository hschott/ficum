package de.bitgrip.ficum.node;

public interface Node {

    void accept(Visitor<?> visitor);

}
