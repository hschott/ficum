package org.ficum.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.conversions.Bson;
import org.ficum.node.AndNode;
import org.ficum.node.Comparison;
import org.ficum.node.ConstraintNode;
import org.ficum.node.Node;
import org.ficum.node.OrNode;

import com.mongodb.client.model.Filters;

public class MongoDBVisitor extends AbstractVisitor<Bson> {

    private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+^$\\\\|]");

    private List<Bson> filters;

    public MongoDBVisitor() {
        super();
    }

    public static String escapeAndConvertWildcards(String value, boolean alwaysWildcard) {
        String ret = SPECIAL_REGEX_CHARS.matcher(value).replaceAll("\\\\$0").replaceAll("\\*", ".*").replaceAll("\\?",
                ".?");
        return alwaysWildcard ? ".*" + ret + ".*" : ret;
    }

    private Bson buildEquals(String fieldName, Comparable<?> argument) {
        Bson pred;
        if (argument instanceof String) {
            final String value = (String) argument;

            if (containsWildcard(value) || isAlwaysWildcard()) {
                String regex = escapeAndConvertWildcards(value, isAlwaysWildcard());
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
                String regex = escapeAndConvertWildcards(value, isAlwaysWildcard());
                pred = Filters.not(Filters.regex(fieldName, regex));
            } else {
                pred = Filters.ne(fieldName, value);
            }
        } else {
            pred = Filters.ne(fieldName, argument);
        }

        return pred;
    }

    private Bson doBuildPredicate(Comparison comparison, String fieldName, Comparable<?> argument) {

        Bson pred = null;
        switch (comparison) {
        case GREATER_THAN:
            pred = Filters.gt(fieldName, argument);
            break;
        case EQUALS:
            pred = buildEquals(fieldName, argument);
            break;
        case NOT_EQUALS:
            pred = buildNotEquals(fieldName, argument);
            break;
        case LESS_THAN:
            pred = Filters.lt(fieldName, argument);
            break;
        case LESS_EQUALS:
            pred = Filters.lte(fieldName, argument);
            break;
        case GREATER_EQUALS:
            pred = Filters.gte(fieldName, argument);
            break;
        default:
            break;
        }
        return pred;
    }

    public Bson start(Node node) {
        filters = new ArrayList<Bson>();
        node.accept(this);
        if (filters.size() != 1) {
            throw new IllegalStateException("single predicate expected, but was: " + filters);
        }
        return filters.get(0);
    }

    public void visit(AndNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        Bson pred = Filters.and(filters.toArray(new Bson[filters.size()]));
        filters.clear();
        filters.add(pred);
    }

    public void visit(ConstraintNode node) {
        Bson pred = doBuildPredicate(node.getComparison(), node.getSelector(), node.getArgument());
        filters.add(pred);
    }

    public void visit(OrNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        Bson pred = Filters.or(filters.toArray(new Bson[filters.size()]));
        filters.clear();
        filters.add(pred);
    }

}
