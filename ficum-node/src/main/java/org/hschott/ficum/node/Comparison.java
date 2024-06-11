package org.hschott.ficum.node;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum Comparison {

    EQUALS("=="), NOT_EQUALS("!="), GREATER_EQUALS("=ge="), LESS_EQUALS("=le="), GREATER_THAN("=gt="), LESS_THAN(
            "=lt="), IN("=in="), NIN("=nin="), NEAR("=nr="), WITHIN("=wi="), INTERSECT("=ix=");

    private static final Map<String, Comparison> lookup = new HashMap<String, Comparison>();

    static {
        for (Comparison comparison : EnumSet.allOf(Comparison.class)) {
            lookup.put(comparison.getSign(), comparison);
        }
    }

    private final String sign;

    private Comparison(String sign) {
        this.sign = sign;
    }

    public static String[] allSigns() {
        String[] signs = new String[lookup.size()];
        lookup.keySet().toArray(signs);
        return signs;
    }

    public static Comparison from(String sign) {
        if (lookup.containsKey(sign)) {
            return lookup.get(sign);
        }
        throw new IllegalArgumentException("Comparison not found for sign: " + sign);
    }

    public String getSign() {
        return sign;
    }
}
