package org.ficum.node;

public enum Operator {

    AND(',', true), OR(';', false), LEFT('(', false), RIGHT(')', true);

    public final char sign;
    public final boolean preceded;

    private Operator(char sign, boolean preceded) {
        this.sign = sign;
        this.preceded = preceded;
    }

}
