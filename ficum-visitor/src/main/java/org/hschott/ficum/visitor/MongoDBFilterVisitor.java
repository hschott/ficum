package org.hschott.ficum.visitor;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.LineString;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import org.bson.conversions.Bson;
import org.hschott.ficum.node.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class MongoDBFilterVisitor extends AbstractVisitor<Bson> {

    private List<Bson> filters;

    private Bson buildEquals(String fieldName, Comparable<?> argument) {
        Bson pred;
        if (argument instanceof String value) {

            if (containsWildcard(value) || isAlwaysWildcard()) {
                String regex = Wildcards.escapeAndConvertToRegexWildcards(value, isAlwaysWildcard());
                pred = Filters.regex(fieldName, regex);
            } else {
                pred = Filters.eq(fieldName, value);
            }
        } else {
            pred = Filters.eq(fieldName, argument);
        }

        return pred;
    }

    private Bson buildNotEquals(String fieldName, Comparable<?> argument) {
        Bson pred;
        if (argument instanceof String value) {

            if (containsWildcard(value) || isAlwaysWildcard()) {
                String regex = Wildcards.escapeAndConvertToRegexWildcards(value, isAlwaysWildcard());
                pred = Filters.not(Filters.regex(fieldName, regex));
            } else {
                pred = Filters.ne(fieldName, value);
            }
        } else {
            pred = Filters.ne(fieldName, argument);
        }

        return pred;
    }

    private Bson doBuildPredicate(Comparison comparison, String fieldName, List<Comparable> comparables) {
        List<Double> geoargs = sanitizeToDouble(comparables);
        switch (comparison) {
            case NEAR:
                if (geoargs.size() == 3) {
                    return Filters.nearSphere(fieldName, new Point(new Position(geoargs.get(0), geoargs.get(1))),
                            geoargs.get(2), null);

                } else if (geoargs.size() == 4) {
                    return Filters.nearSphere(fieldName, new Point(new Position(geoargs.get(0), geoargs.get(1))),
                            geoargs.get(2), geoargs.get(3));

                }
                break;

            case WITHIN:
                switch (geoargs.size()) {
                    case 0:
                    case 1:
                    case 2:
                        break;

                    case 3:
                        return Filters.geoWithinCenterSphere(fieldName, geoargs.get(0), geoargs.get(1), geoargs.get(2));

                    case 4:
                        return Filters.geoWithinBox(fieldName, geoargs.get(0), geoargs.get(1), geoargs.get(2),
                                geoargs.get(3));

                    default:
                        Polygon geometry = new Polygon(toPositions(geoargs, true));
                        return Filters.geoWithin(fieldName, geometry);
                }
                break;

            case INTERSECT:
                switch (geoargs.size()) {
                    case 0:
                    case 1:
                    case 3:
                        break;

                    case 2:
                        return Filters.geoIntersects(fieldName, new Point(new Position(geoargs.get(0), geoargs.get(1))));

                    case 4:
                        return Filters.geoIntersects(fieldName, new LineString(toPositions(geoargs, false)));

                    default:
                        Polygon geometry = new Polygon(toPositions(geoargs, true));
                        return Filters.geoIntersects(fieldName, geometry);
                }
                break;

            case IN:
                return Filters.in(fieldName, comparables);

            case NIN:
                return Filters.nin(fieldName, comparables);

            default:
                break;
        }
        return null;
    }

    private Bson doBuildPredicate(Comparison comparison, String fieldName, Comparable<?> argument) {
        return switch (comparison) {
            case GREATER_THAN -> Filters.gt(fieldName, argument);
            case EQUALS -> buildEquals(fieldName, argument);
            case NOT_EQUALS -> buildNotEquals(fieldName, argument);
            case LESS_THAN -> Filters.lt(fieldName, argument);
            case LESS_EQUALS -> Filters.lte(fieldName, argument);
            case GREATER_EQUALS -> Filters.gte(fieldName, argument);
            case IN, NIN -> doBuildPredicate(comparison, fieldName, Collections.singletonList(argument));
            default -> null;
        };
    }

    public Bson start(Node node) {
        filters = new ArrayList<>();
        node.accept(this);
        if (filters.size() != 1) {
            throw new IllegalStateException("single predicate expected, but was: " + filters);
        }
        return filters.getFirst();
    }

    private List<Position> toPositions(List<Double> arguments, boolean close) {
        Iterator<Double> it = arguments.iterator();
        List<Position> positions = new ArrayList<>();

        while (it.hasNext()) {
            Double lon = it.next();
            if (it.hasNext()) {
                Double lat = it.next();
                positions.add(new Position(lon, lat));
            }
        }

        if (close && positions.size() >= 3 && !positions.getFirst().equals(positions.getLast())) {
            positions.add(positions.getFirst());
        }

        return positions;
    }

    private List<Double> sanitizeToDouble(List<Comparable> arguments) {
        return arguments.stream().filter(Double.class::isInstance).map(Double.class::cast).collect(Collectors.toList());
    }

    public void visit(ConstraintNode<?> node) {
        Object argument = node.getArgument();
        String fieldName = getMappedField(node.getSelector());

        Bson pred;
        switch (argument) {
            case Comparable<?> comparable -> {
                Comparable<?> value = comparable;

                if (value instanceof OffsetDateTime) {
                    value = ((OffsetDateTime) value).toLocalDateTime();
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

        Bson pred;
        Bson leftHandSide = filters.get(filters.size() - 2);
        Bson rightHandSide = filters.get(filters.size() - 1);
        pred = switch (node.getOperator()) {
            case AND -> Filters.and(leftHandSide, rightHandSide);
            case OR -> Filters.or(leftHandSide, rightHandSide);
            case NAND -> Filters.or(Filters.not(leftHandSide), Filters.not(rightHandSide));
            case NOR -> Filters.and(Filters.not(leftHandSide), Filters.not(rightHandSide));
            default ->
                    throw new IllegalArgumentException("OperationNode: " + node + " does not resolve to a operation");
        };

        filters.remove(leftHandSide);
        filters.remove(rightHandSide);
        filters.add(pred);
    }

}
