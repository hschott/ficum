package org.ficum.visitor;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
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

    public static final char ESCAPE_CHAR = '\\';

    private EntityManager entityManager;

    private CriteriaBuilder builder;

    private Root<T> root;

    private Class<T> queryClass;

    private List<Predicate> predicates;

    private boolean distinct = true;

    private Set<Class<?>> mappedTypes = new HashSet<Class<?>>();

    public JPATypedQueryVisitor() {
        super();
    }

    public JPATypedQueryVisitor(Class<T> queryClass, EntityManager entityManager) {
        super();
        this.queryClass = queryClass;
        this.entityManager = entityManager;
        mappedTypes.add(String.class);
        mappedTypes.add(Character.class);
        mappedTypes.add(Boolean.class);
        mappedTypes.add(Number.class);
        mappedTypes.add(Date.class);
        mappedTypes.add(Calendar.class);
        mappedTypes.add(Enum.class);
    }

    private static boolean containsEscapedChar(String value) {
        return value.contains("\\%") || value.contains("\\\\") || value.contains("\\_");
    }

    protected static String escapeAndConvertWildcards(String value, boolean alwaysWildcard) {
        String ret = value.replaceAll("\\\\", "\\\\\\\\") // escape 'sql escape'
                                                          // char
                .replaceAll("_", "\\\\_").replaceAll("%", "\\\\%") // escape sql
                                                                   // wildcards
                .replaceAll("\\*", "%").replaceAll("\\?", "_"); // replace rql
                                                                // wildcard with
                                                                // sql wildcard

        return alwaysWildcard ? "%" + ret + "%" : ret;
    }

    public boolean addMappedType(Class<?> mappedType) {
        return mappedTypes.add(mappedType);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate buildEquals(Object value, Expression<? extends Comparable> path) {
        Predicate pred;
        if (path.getJavaType().equals(String.class)) {
            final String originalValue = value.toString();

            if (containsWildcard(originalValue) || isAlwaysWildcard()) {
                String theValue = escapeAndConvertWildcards(originalValue, isAlwaysWildcard());
                if (containsEscapedChar(theValue)) {
                    pred = builder.like((Expression<String>) path, theValue, ESCAPE_CHAR);
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
                String theValue = escapeAndConvertWildcards(originalValue, isAlwaysWildcard());
                if (containsEscapedChar(theValue)) {
                    pred = builder.notLike((Expression<String>) path, theValue, ESCAPE_CHAR);
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

        switch (comparison) {
        case GREATER_THAN:
            return builder.greaterThan(path, argument);

        case EQUALS:
            return buildEquals(argument, path);

        case NOT_EQUALS:
            return buildNotEquals(argument, path);

        case LESS_THAN:
            return builder.lessThan(path, argument);

        case LESS_EQUALS:
            return builder.lessThanOrEqualTo(path, argument);

        case GREATER_EQUALS:
            return builder.greaterThanOrEqualTo(path, argument);

        default:
            return null;
        }
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
        return isCollection(path.getJavaType()) && argument instanceof Integer;
    }

    public boolean isDistinct() {
        return distinct;
    }

    protected boolean isMappedType(Class<?> clazz) {
        for (Class<?> mappedType : mappedTypes) {
            if (mappedType.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
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

        return entityManager.createQuery(
                cq.select(root).distinct(isDistinct()).where(predicates.toArray(new Predicate[predicates.size()])));
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
        Object argument = node.getArgument();

        if (argument instanceof Comparable<?>) {
            Comparable<?> value = (Comparable<?>) argument;

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

            if (pred != null) {
                predicates.add(pred);
            } else {
                throw new IllegalArgumentException("Constraint: " + node + " does not resolve to a predicate");
            }

        } else {
            throw new IllegalArgumentException("Unable to handle argument of type " + argument.getClass().getName());
        }

    }

    public void visit(OrNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        Predicate pred = builder.or(predicates.toArray(new Predicate[predicates.size()]));
        predicates.clear();
        predicates.add(pred);
    }

}
