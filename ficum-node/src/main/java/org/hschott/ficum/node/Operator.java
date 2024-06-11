package org.hschott.ficum.node;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum Operator {

    AND(",", true), NAND(".", false), OR(";", false), NOR(":", true), LEFT("(", false), RIGHT(")", true);

    private static final Map<String, Operator> lookup = new HashMap<String, Operator>();

    static {
        for (Operator operator : EnumSet.allOf(Operator.class)) {
            lookup.put(operator.getSign(), operator);
        }
    }

    private final String sign;

    public final boolean preceded;

    private Operator(String sign, boolean preceded) {
        this.sign = sign;
        this.preceded = preceded;
    }

    public static String[] allSigns() {
        String[] signs = new String[lookup.size()];
        lookup.keySet().toArray(signs);
        return signs;
    }

    public static Operator from(String sign) {
        if (lookup.containsKey(sign)) {
            return lookup.get(sign);
        }
        throw new IllegalArgumentException("Operator not found for sign: " + sign);
    }

    public String getSign() {
        return sign;
    }
}
