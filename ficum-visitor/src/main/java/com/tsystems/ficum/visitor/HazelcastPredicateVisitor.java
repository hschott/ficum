package com.tsystems.ficum.visitor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import com.tsystems.ficum.node.AbstractVisitor;
import com.tsystems.ficum.node.Comparison;
import com.tsystems.ficum.node.ConstraintNode;
import com.tsystems.ficum.node.Node;
import com.tsystems.ficum.node.OperationNode;

public class HazelcastPredicateVisitor extends AbstractVisitor<Predicate<?, ?>> {

    private List<Predicate<?, ?>> filters;

    private Predicate<?, ?> buildEquals(String fieldName, Comparable<?> argument) {
        Predicate<?, ?> pred;
        if (argument instanceof String) {
            final String value = (String) argument;

            if (containsWildcard(value) || isAlwaysWildcard()) {
                String like = Wildcards.escapeAndConvertToSQLWildcards(value, isAlwaysWildcard());
                pred = Predicates.like(fieldName, like);
            } else {
                pred = Predicates.equal(fieldName, value);
            }
        } else {
            pred = Predicates.equal(fieldName, argument);
        }

        return pred;
    }

    private Predicate<?, ?> buildNotEquals(String fieldName, Comparable<?> argument) {
        Predicate<?, ?> pred;
        if (argument instanceof String) {
            final String value = (String) argument;

            if (containsWildcard(value) || isAlwaysWildcard()) {
                String like = Wildcards.escapeAndConvertToSQLWildcards(value, isAlwaysWildcard());
                pred = Predicates.not(Predicates.like(fieldName, like));
            } else {
                pred = Predicates.notEqual(fieldName, value);
            }
        } else {
            pred = Predicates.notEqual(fieldName, argument);
        }

        return pred;
    }

    private Predicate<?, ?> doBuildPredicate(Comparison comparison, String fieldName, Comparable<?> argument) {
        switch (comparison) {
        case GREATER_THAN:
            return Predicates.greaterThan(fieldName, argument);

        case EQUALS:
            return buildEquals(fieldName, argument);

        case NOT_EQUALS:
            return buildNotEquals(fieldName, argument);

        case LESS_THAN:
            return Predicates.lessThan(fieldName, argument);

        case LESS_EQUALS:
            return Predicates.lessEqual(fieldName, argument);

        case GREATER_EQUALS:
            return Predicates.greaterEqual(fieldName, argument);

        default:
            return null;
        }
    }

    public Predicate<?, ?> start(Node node) {
        filters = new ArrayList<Predicate<?, ?>>();
        node.accept(this);
        if (filters.size() != 1) {
            throw new IllegalStateException("single predicate expected, but was: " + filters);
        }
        return filters.get(0);
    }

    public void visit(ConstraintNode<?> node) {
        Object argument = node.getArgument();
        String fieldName = getMappedField(node.getSelector());

        Predicate<?, ?> pred = null;
        if (argument instanceof Comparable<?>) {
            Comparable<?> value = (Comparable<?>) argument;

            if (argument instanceof Calendar) {
                value = ((Calendar) value).getTime();
            }

            pred = doBuildPredicate(node.getComparison(), fieldName, value);

        } else {
            throw new IllegalArgumentException("Unable to handle argument of type " + argument.getClass().getName());
        }

        if (pred != null) {
            filters.add(pred);
        } else {
            throw new IllegalArgumentException("Constraint: " + node + " does not resolve to a predicate");
        }
    }

    public void visit(OperationNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);

        Predicate<?, ?> pred = null;
        switch (node.getOperator()) {
        case AND:
            pred = Predicates.and(filters.get(0), filters.get(1));
            break;

        case OR:
            pred = Predicates.or(filters.get(0), filters.get(1));
            break;

        case NAND:
            pred = Predicates.or(Predicates.not(filters.get(0)), Predicates.not(filters.get(1)));
            break;

        case NOR:
            pred = Predicates.and(Predicates.not(filters.get(0)), Predicates.not(filters.get(1)));
            break;

        default:
            throw new IllegalArgumentException("OperationNode: " + node + " does not resolve to a operation");
        }

        filters.clear();
        filters.add(pred);
    }

}
