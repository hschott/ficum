package de.bitgrip.ficum.visitor;

import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import de.bitgrip.ficum.node.*;

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

            case IN:
            case NIN:
                return doBuildPredicate(comparison, fieldName, Collections.singletonList(argument));

            default:
                return null;
        }
    }

    private Predicate<?, ?> doBuildPredicate(Comparison comparison, String fieldName, List<Comparable> arguments) {
        switch (comparison) {
            case IN:
                return Predicates.in(fieldName, arguments.toArray(new Comparable[arguments.size()]));

            case NIN:
                return Predicates.not(Predicates.in(fieldName, arguments.toArray(new Comparable[arguments.size()])));

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

            if (argument instanceof LocalDate) {
                value = Date.from(((LocalDate) value).atStartOfDay().atZone(ZoneId.of("UTC")).toInstant());
            }

            if (argument instanceof OffsetDateTime) {
                value = Date.from(((OffsetDateTime) value).toInstant());
            }

            pred = doBuildPredicate(node.getComparison(), fieldName, value);

        } else if (argument instanceof List) {
            pred = doBuildPredicate(node.getComparison(), fieldName, sanitizeToComparable((List) argument));

        } else if (argument == null) {
            pred = doBuildPredicate(node.getComparison(), fieldName, (Comparable<?>) null);
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
        Predicate<?, ?> leftHandSide = filters.get(filters.size() - 2);
        Predicate<?, ?> rightHandSide = filters.get(filters.size() - 1);
        switch (node.getOperator()) {
            case AND:
                pred = Predicates.and(leftHandSide, rightHandSide);
                break;

            case OR:
                pred = Predicates.or(leftHandSide, rightHandSide);
                break;

            case NAND:
                pred = Predicates.or(Predicates.not(leftHandSide), Predicates.not(rightHandSide));
                break;

            case NOR:
                pred = Predicates.and(Predicates.not(leftHandSide), Predicates.not(rightHandSide));
                break;

            default:
                throw new IllegalArgumentException("OperationNode: " + node + " does not resolve to a operation");
        }

        filters.remove(leftHandSide);
        filters.remove(rightHandSide);
        filters.add(pred);
    }

}
