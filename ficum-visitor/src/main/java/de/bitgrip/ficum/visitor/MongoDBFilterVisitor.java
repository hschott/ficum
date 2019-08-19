package de.bitgrip.ficum.visitor;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.LineString;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import de.bitgrip.ficum.node.*;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

public class MongoDBFilterVisitor extends AbstractVisitor<Bson> {

    private List<Bson> filters;

    private Bson buildEquals(String fieldName, Comparable<?> argument) {
        Bson pred;
        if (argument instanceof String) {
            final String value = (String) argument;

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
        if (argument instanceof String) {
            final String value = (String) argument;

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
        List<Double> geoargs = sanatizeToDouble(comparables);
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
                        @SuppressWarnings("unchecked")
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
                        @SuppressWarnings("unchecked")
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
        switch (comparison) {
            case GREATER_THAN:
                return Filters.gt(fieldName, argument);

            case EQUALS:
                return buildEquals(fieldName, argument);

            case NOT_EQUALS:
                return buildNotEquals(fieldName, argument);

            case LESS_THAN:
                return Filters.lt(fieldName, argument);

            case LESS_EQUALS:
                return Filters.lte(fieldName, argument);

            case GREATER_EQUALS:
                return Filters.gte(fieldName, argument);

            default:
                return null;
        }
    }

    public Bson start(Node node) {
        filters = new ArrayList<Bson>();
        node.accept(this);
        if (filters.size() != 1) {
            throw new IllegalStateException("single predicate expected, but was: " + filters);
        }
        return filters.get(0);
    }

    private List<Position> toPositions(List<Double> arguments, boolean close) {
        Iterator<Double> it = arguments.iterator();
        List<Position> positions = new ArrayList<Position>();

        while (it.hasNext()) {
            Double lon = it.next();
            if (it.hasNext()) {
                Double lat = it.next();
                positions.add(new Position(lon, lat));
            }
        }

        if (close && positions.size() >= 3 && !positions.get(0).equals(positions.get(positions.size() - 1))) {
            positions.add(positions.get(0));
        }

        return positions;
    }

    private List<Double> sanatizeToDouble(List<Comparable> arguments) {
        Iterator<Double> value = Iterators.filter(arguments.iterator(), Double.class);
        return Lists.newArrayList(value);
    }

    public void visit(ConstraintNode<?> node) {
        Object argument = node.getArgument();
        String fieldName = getMappedField(node.getSelector());

        Bson pred = null;
        if (argument instanceof Comparable<?>) {
            Comparable<?> value = (Comparable<?>) argument;

            if (argument instanceof Calendar) {
                value = ((Calendar) value).getTime();
            }
            pred = doBuildPredicate(node.getComparison(), fieldName, value);

        } else if (argument instanceof List) {
            pred = doBuildPredicate(node.getComparison(), fieldName, sanatizeToComparable((List) argument));
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

        Bson pred = null;
        switch (node.getOperator()) {
            case AND:
                pred = Filters.and(filters.get(0), filters.get(1));
                break;

            case OR:
                pred = Filters.or(filters.get(0), filters.get(1));
                break;

            case NAND:
                pred = Filters.or(Filters.not(filters.get(0)), Filters.not(filters.get(1)));
                break;

            case NOR:
                pred = Filters.and(Filters.not(filters.get(0)), Filters.not(filters.get(1)));
                break;

            default:
                throw new IllegalArgumentException("OperationNode: " + node + " does not resolve to a operation");
        }

        filters.clear();
        filters.add(pred);
    }

}
