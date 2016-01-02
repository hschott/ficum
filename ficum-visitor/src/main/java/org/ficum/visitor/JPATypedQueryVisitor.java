package org.ficum.visitor;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.ficum.node.AndNode;
import org.ficum.node.Comparison;
import org.ficum.node.ConstraintNode;
import org.ficum.node.Node;
import org.ficum.node.OrNode;

public class JPATypedQueryVisitor<T> extends AbstractVisitor<TypedQuery<T>> {

    private EntityManager entityManager;

    private CriteriaBuilder builder;

    private Root<T> root;

    private Class<T> queryClass;

    private List<Predicate> predicates;

    public JPATypedQueryVisitor() {
        super();
    }

    public JPATypedQueryVisitor(Class<T> queryClass, EntityManager entityManager) {
        super();
        this.queryClass = queryClass;
        this.entityManager = entityManager;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate buildEquals(Object value, Expression<? extends Comparable> path) {
        Predicate pred;
        if (path.getJavaType().equals(String.class)) {
            final String originalValue = value.toString();

            if (containsWildcard(originalValue) || isAlwaysWildcard()) {
                String theValue = SQLEscapeUtils.escapeAndConvertWildcards(originalValue, isAlwaysWildcard());
                if (SQLEscapeUtils.containsEscapedChar(theValue)) {
                    pred = builder.like((Expression<String>) path, theValue, SQLEscapeUtils.ESCAPE_CHAR);
                } else {
                    pred = builder.like((Expression<String>) path, theValue);
                }
            } else {
                pred = builder.equal(path, originalValue);
            }
        } else {
            pred = builder.equal(path, value);
        }
        return pred;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate buildNotEquals(Object value, Expression<? extends Comparable> path) {
        Predicate pred;
        if (path.getJavaType().equals(String.class)) {
            final String originalValue = value.toString();

            if (containsWildcard(originalValue) || isAlwaysWildcard()) {
                String theValue = SQLEscapeUtils.escapeAndConvertWildcards(originalValue, isAlwaysWildcard());
                if (SQLEscapeUtils.containsEscapedChar(theValue)) {
                    pred = builder.notLike((Expression<String>) path, theValue, SQLEscapeUtils.ESCAPE_CHAR);
                } else {
                    pred = builder.notLike((Expression<String>) path, theValue);
                }
            } else {
                pred = builder.notEqual(path, originalValue);
            }
        } else {
            pred = builder.notEqual(path, value);
        }
        return pred;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate doBuildCollectionSizePredicate(Comparison comparison, Path<?> path, Integer argument) {
        Expression<Integer> exp = builder.size((Expression<? extends Collection>) path);

        switch (comparison) {
        case GREATER_THAN:
            return builder.greaterThan(exp, argument);
        case EQUALS:
            return builder.equal(exp, argument);
        case NOT_EQUALS:
            return builder.notEqual(exp, argument);
        case LESS_THAN:
            return builder.lessThan(exp, argument);
        case LESS_EQUALS:
            return builder.lessThanOrEqualTo(exp, argument);
        case GREATER_EQUALS:
            return builder.greaterThanOrEqualTo(exp, argument);
        default:
            return null;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate doBuildPredicate(Comparison comparison, Expression<? extends Comparable> path,
            Comparable<?> argument) {

        Predicate pred = null;
        switch (comparison) {
        case GREATER_THAN:
            pred = builder.greaterThan(path, argument);
            break;
        case EQUALS:
            pred = buildEquals(argument, path);
            break;
        case NOT_EQUALS:
            pred = buildNotEquals(argument, path);
            break;
        case LESS_THAN:
            pred = builder.lessThan(path, argument);
            break;
        case LESS_EQUALS:
            pred = builder.lessThanOrEqualTo(path, argument);
            break;
        case GREATER_EQUALS:
            pred = builder.greaterThanOrEqualTo(path, argument);
            break;
        default:
            break;
        }
        return pred;
    }

    private Path<?> findPath(String names) {
        Path<?> path = root;
        Class<?> clazz = queryClass;

        Iterator<String> namesIterator = Arrays.asList(names.split("\\.")).iterator();
        while (namesIterator.hasNext()) {
            String name = namesIterator.next();
            boolean isLast = !namesIterator.hasNext();

            Field field = getField(clazz, name);
            clazz = field.getType();

            if (isMappedType(clazz)) {
                if (isLast)
                    path = path.get(name);
                else
                    throw new IllegalArgumentException(String
                            .format("%s resolves to %s and can not contain a nested property", name, clazz.getName()));
            } else if (isCollection(clazz)) {
                clazz = getGenericTypeClazz((ParameterizedType) field.getGenericType());
                if (isLast && !isMappedType(clazz))
                    path = path.get(name);
                else {
                    path = getOrCreateJoin(path, name);
                }
            } else if (path instanceof From) {
                path = getOrCreateJoin(path, name);
            }
        }

        if (path == null)
            throw new IllegalArgumentException(
                    String.format("%s can not be applied to %s", names, queryClass.getName()));

        return path;
    }

    private Path<?> getExistingJoin(From<?, ?> element, String prop) {
        final Set<?> joins = element.getJoins();
        for (Object object : joins) {
            Join<?, ?> join = (Join<?, ?>) object;
            if (join.getAttribute().getName().equals(prop)) {
                return join;
            }
        }
        return null;
    }

    private Path<?> getOrCreateJoin(Path<?> path, String name) {
        Path<?> ret = getExistingJoin((From<?, ?>) path, name);
        if (ret != null) {
            return ret;
        } else {
            return path.equals(root) ? root.join(name) : ((From<?, ?>) path).join(name);
        }
    }

    private boolean isCollectionSizeCheck(Path<?> path, Comparable<?> argument) {
        if (isCollection(path.getJavaType()) && argument instanceof Integer) {
            return true;
        } else {
            return false;
        }
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void setQueryClass(Class<T> queryClass) {
        this.queryClass = queryClass;
    }

    public TypedQuery<T> start(Node node) {
        if (entityManager == null) {
            throw new IllegalStateException("EntityManager can not be null.");
        }
        if (queryClass == null) {
            throw new IllegalStateException("QueryClass can not be null.");
        }

        builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = builder.createQuery(queryClass);
        root = cq.from(queryClass);
        predicates = new ArrayList<Predicate>();

        node.accept(this);

        CriteriaQuery<T> select = cq.select(root).distinct(isDistinct());
        select.where(predicates.toArray(new Predicate[predicates.size()]));

        return entityManager.createQuery(cq);
    }

    public void visit(AndNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        Predicate pred = builder.and(predicates.toArray(new Predicate[predicates.size()]));
        predicates.clear();
        predicates.add(pred);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void visit(ConstraintNode node) {
        Path<?> path = findPath(node.getSelector());

        Class<? extends Comparable> clazz = (Class<? extends Comparable>) path.getJavaType();
        Comparable<?> value = node.getArgument();
        if (clazz.isEnum()) {
            value = Enum.valueOf((Class<? extends Enum>) clazz, value.toString());
        }

        if (value instanceof Calendar && clazz.isAssignableFrom(Date.class)) {
            Calendar cal = (Calendar) value;
            value = cal.getTime();
        }

        Predicate pred = isCollectionSizeCheck(path, value)
                ? doBuildCollectionSizePredicate(node.getComparison(), path, (Integer) value)
                : doBuildPredicate(node.getComparison(), path.as(clazz), value);

        predicates.add(pred);
    }

    public void visit(OrNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        Predicate pred = builder.or(predicates.toArray(new Predicate[predicates.size()]));
        predicates.clear();
        predicates.add(pred);
    }

}
