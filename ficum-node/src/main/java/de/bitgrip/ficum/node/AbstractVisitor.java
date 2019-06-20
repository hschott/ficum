package de.bitgrip.ficum.node;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractVisitor<T> implements Visitor<T> {

    private boolean alwaysWildcard = false;

    private Map<String, String> selectorToFieldMapping = new HashMap<String, String>();

    public static boolean containsWildcard(String value) {
        return value.contains("*") || value.contains("?");
    }

    public void addSelectorToFieldMapping(String selector, String field) {
        selectorToFieldMapping.put(selector, field);
    }

    public String getMappedField(String selector) {
        if (selectorToFieldMapping.containsKey(selector)) {
            return selectorToFieldMapping.get(selector);
        } else {
            return selector;
        }
    }

    public boolean isAlwaysWildcard() {
        return alwaysWildcard;
    }

    public void setAlwaysWildcard(boolean alwaysWildcardMatch) {
        this.alwaysWildcard = alwaysWildcardMatch;
    }

    public void setSelectorToFieldMapping(Map<String, String> selectorToFieldMapping) {
        this.selectorToFieldMapping = selectorToFieldMapping;
    }

    public void visit(Node node) {
        if (node instanceof ConstraintNode) {
            visit((ConstraintNode<?>) node);
            return;
        }
        if (node instanceof OperationNode) {
            visit((OperationNode) node);
            return;
        }
    }

}
