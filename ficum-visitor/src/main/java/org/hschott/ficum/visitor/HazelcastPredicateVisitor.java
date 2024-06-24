package org.hschott.ficum.visitor;

import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.hschott.ficum.node.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HazelcastPredicateVisitor extends AbstractVisitor<Predicate<?, ?>> {

    private List<Predicate<?, ?>> filters;

    private Predicate<?, ?> buildEquals(String fieldName, Comparable<?> argument) {
        Predicate<?, ?> pred;
        if (argument instanceof String value) {
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
        if (argument instanceof String value) {
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
        return switch (comparison) {
            case GREATER_THAN -> Predicates.greaterThan(fieldName, argument);
            case EQUALS -> buildEquals(fieldName, argument);
            case NOT_EQUALS -> buildNotEquals(fieldName, argument);
            case LESS_THAN -> Predicates.lessThan(fieldName, argument);
            case LESS_EQUALS -> Predicates.lessEqual(fieldName, argument);
            case GREATER_EQUALS -> Predicates.greaterEqual(fieldName, argument);
            case IN, NIN -> doBuildPredicate(comparison, fieldName, Collections.singletonList(argument));
            default -> null;
        };
    }

    private Predicate<?, ?> doBuildPredicate(Comparison comparison, String fieldName, List<Comparable> arguments) {
        return switch (comparison) {
            case IN -> Predicates.in(fieldName, arguments.toArray(new Comparable[0]));
            case NIN -> Predicates.not(Predicates.in(fieldName, arguments.toArray(new Comparable[0])));
            default -> null;
        };
    }

    public Predicate<?, ?> start(Node node) {
        filters = new ArrayList<>();
        node.accept(this);
        if (filters.size() != 1) {
            throw new IllegalStateException("single predicate expected, but was: " + filters);
        }
        return filters.getFirst();
    }

    public void visit(ConstraintNode<?> node) {
        Object argument = node.getArgument();
        String fieldName = getMappedField(node.getSelector());

        Predicate<?, ?> pred;
        switch (argument) {
            case Comparable<?> value -> {
                if (argument instanceof LocalDate localDate) {
                    value = Date.from((localDate).atStartOfDay().atZone(ZoneId.of("UTC")).toInstant());
                }
                if (argument instanceof OffsetDateTime offsetDateTime) {
                    value = Date.from((offsetDateTime).toInstant());
                }
                pred = doBuildPredicate(node.getComparison(), fieldName, value);
            }
            case List list -> pred = doBuildPredicate(node.getComparison(), fieldName, sanitizeToComparable(list));
            case null -> pred = doBuildPredicate(node.getComparison(), fieldName, (Comparable<?>) null);
            default -> throw new IllegalArgumentException(
                    "Unable to handle argument of type " + argument.getClass().getName());
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

        Predicate<?, ?> pred;
        Predicate<?, ?> leftHandSide = filters.get(filters.size() - 2);
        Predicate<?, ?> rightHandSide = filters.get(filters.size() - 1);
        pred = switch (node.getOperator()) {
            case AND -> Predicates.and(leftHandSide, rightHandSide);
            case OR -> Predicates.or(leftHandSide, rightHandSide);
            case NAND -> Predicates.or(Predicates.not(leftHandSide), Predicates.not(rightHandSide));
            case NOR -> Predicates.and(Predicates.not(leftHandSide), Predicates.not(rightHandSide));
            default ->
                    throw new IllegalArgumentException("OperationNode: " + node + " does not resolve to a operation");
        };

        filters.remove(leftHandSide);
        filters.remove(rightHandSide);
        filters.add(pred);
    }

}
