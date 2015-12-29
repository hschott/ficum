package org.ficum.node;

public enum Comparison {

    EQUALS("=="), NOT_EQUALS("!="), GREATER_EQUALS("=ge="), LESS_EQUALS("=le="), GREATER_THAN("=gt="), LESS_THAN(
            "=lt=");

    public final String sign;

    private Comparison(String sign) {
        this.sign = sign;
    }

}
