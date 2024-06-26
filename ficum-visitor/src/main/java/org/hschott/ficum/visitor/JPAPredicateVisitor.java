package org.hschott.ficum.visitor;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.hschott.ficum.node.*;

import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class JPAPredicateVisitor<T> extends AbstractVisitor<Predicate> {

    public static final char ESCAPE_CHAR = '\\';

    private final CriteriaBuilder criteriaBuilder;

    private final Root<T> root;

    private final Class<T> queryClass;

    private List<Predicate> predicates;

    private final Set<Class<? extends Comparable<?>>> mappedTypes = new HashSet<>();

    public JPAPredicateVisitor(Class<T> queryClass, Root<T> root, CriteriaBuilder criteriaBuilder) {
        super();
        this.queryClass = queryClass;
        this.criteriaBuilder = criteriaBuilder;
        this.root = root;
        mappedTypes.add(String.class);
        mappedTypes.add(Character.class);
        mappedTypes.add(Boolean.class);
        mappedTypes.add(Double.class);
        mappedTypes.add(Integer.class);
        mappedTypes.add(Float.class);
        mappedTypes.add(Short.class);
        mappedTypes.add(Long.class);
        mappedTypes.add(Byte.class);
        mappedTypes.add(BigInteger.class);
        mappedTypes.add(BigDecimal.class);
        mappedTypes.add(Date.class);
        mappedTypes.add(Calendar.class);
        mappedTypes.add(UUID.class);
        mappedTypes.add(OffsetDateTime.class);
        mappedTypes.add(LocalDate.class);
    }

    private static boolean containsEscapedChar(String value) {
        return value.contains("\\%") || value.contains("\\\\") || value.contains("\\_");
    }

    public boolean addMappedType(Class<? extends Comparable<?>> mappedType) {
        return mappedTypes.add(mappedType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildEquals(Comparable value, Expression<? extends Comparable> path) {
        Predicate pred;
        if (value == null) {
            pred = path.isNull();
        } else if (String.class.isAssignableFrom(path.getJavaType())) {
            final String originalValue = value.toString();

            if (containsWildcard(originalValue) || isAlwaysWildcard()) {
                String theValue = Wildcards.escapeAndConvertToSQLWildcards(originalValue, isAlwaysWildcard());
                if (containsEscapedChar(theValue)) {
                    pred = criteriaBuilder.like((Expression<String>) path, theValue, ESCAPE_CHAR);
                } else {
                    pred = criteriaBuilder.like((Expression<String>) path, theValue);
                }
            } else {
                pred = criteriaBuilder.equal(path, originalValue);
            }
        } else {
            pred = criteriaBuilder.equal(path, value);
        }
        return pred;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildNotEquals(Comparable value, Expression<? extends Comparable> path) {
        Predicate pred;
        if (value == null) {
            pred = path.isNotNull();
        } else if (String.class.isAssignableFrom(path.getJavaType())) {
            final String originalValue = value.toString();

            if (containsWildcard(originalValue) || isAlwaysWildcard()) {
                String theValue = Wildcards.escapeAndConvertToSQLWildcards(originalValue, isAlwaysWildcard());
                if (containsEscapedChar(theValue)) {
                    pred = criteriaBuilder.notLike((Expression<String>) path, theValue, ESCAPE_CHAR);
                } else {
                    pred = criteriaBuilder.notLike((Expression<String>) path, theValue);
                }
            } else {
                pred = criteriaBuilder.notEqual(path, originalValue);
            }
        } else {
            pred = criteriaBuilder.notEqual(path, value);
        }
        return pred;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate doBuildCollectionSizePredicate(Comparison comparison, Path<?> path, Integer argument) {
        Expression<Integer> exp = criteriaBuilder.size((Expression<? extends Collection>) path);

        return switch (comparison) {
            case GREATER_THAN -> criteriaBuilder.greaterThan(exp, argument);
            case EQUALS -> criteriaBuilder.equal(exp, argument);
            case NOT_EQUALS -> criteriaBuilder.notEqual(exp, argument);
            case LESS_THAN -> criteriaBuilder.lessThan(exp, argument);
            case LESS_EQUALS -> criteriaBuilder.lessThanOrEqualTo(exp, argument);
            case GREATER_EQUALS -> criteriaBuilder.greaterThanOrEqualTo(exp, argument);
            default -> null;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate doBuildPredicate(Comparison comparison, Expression<? extends Comparable> path,
                                       Comparable argument) {

        return switch (comparison) {
            case GREATER_THAN -> criteriaBuilder.greaterThan(path, argument);
            case EQUALS -> buildEquals(argument, path);
            case NOT_EQUALS -> buildNotEquals(argument, path);
            case LESS_THAN -> criteriaBuilder.lessThan(path, argument);
            case LESS_EQUALS -> criteriaBuilder.lessThanOrEqualTo(path, argument);
            case GREATER_EQUALS -> criteriaBuilder.greaterThanOrEqualTo(path, argument);
            case IN, NIN -> doBuildPredicate(comparison, path, Collections.singletonList(argument));
            default -> null;
        };
    }

    @SuppressWarnings({"rawtypes"})
    private Predicate doBuildPredicate(Comparison comparison, Expression<? extends Comparable> path,
                                       List<Comparable> argument) {

        return switch (comparison) {
            case IN -> path.in(argument);
            case NIN -> criteriaBuilder.not(path.in(argument));
            default -> null;
        };
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
        final Set<? extends Join<?, ?>> joins = element.getJoins();
        for (Join<?, ?> join : joins) {
            if (join.getAttribute().getName().equals(prop)) {
                return join;
            }
        }
        return null;
    }

    private Field getField(Class<?> clazz, String name) {
        Field field = FieldUtils.getField(clazz, name, true);
        if (field == null) {
            throw new IllegalArgumentException(String.format("Can not find field %s in %s", name, clazz.getName()));
        }
        return field;
    }

    private Class<?> getGenericTypeClazz(ParameterizedType type) {
        Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(type);
        Iterator<Type> it = typeArguments.values().iterator();
        if (it.hasNext()) {
            return TypeUtils.getRawType(it.next(), null);
        }
        return Object.class;
    }

    private Path<?> getOrCreateJoin(Path<?> path, String name) {
        Path<?> ret = getExistingJoin((From<?, ?>) path, name);
        if (ret != null) {
            return ret;
        } else {
            return path.equals(root) ? root.join(name) : ((From<?, ?>) path).join(name);
        }
    }

    private boolean isCollection(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
    }

    private boolean isCollectionSizeCheck(Path<?> path, Comparable<?> argument) {
        return isCollection(path.getJavaType()) && argument instanceof Integer;
    }

    private boolean isMappedType(Class<?> clazz) {
        for (Class<? extends Comparable<?>> mappedType : mappedTypes) {
            if (mappedType.isAssignableFrom(clazz) || clazz.isEnum()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Comparable<?> convertValue(Comparable<?> value,  Class<? extends Comparable> clazz ){
        // convert from string to enum
        if (value instanceof String && clazz.isEnum()) {
            value = Enum.valueOf((Class<? extends Enum>) clazz, value.toString());
        }

        // convert date and time types
        if (value instanceof LocalDate && clazz.isAssignableFrom(Date.class)) {
            value = Date.from(((LocalDate) value).atStartOfDay()
                    .atZone(ZoneId.of("UTC"))
                    .toInstant());
        }

        if (value instanceof LocalDate && clazz.isAssignableFrom(Calendar.class)) {
            value = GregorianCalendar.from(((LocalDate) value).atStartOfDay()
                    .atZone(ZoneId.of("UTC")));
        }

        if (value instanceof LocalDate && clazz.isAssignableFrom(OffsetDateTime.class)) {
            value = OffsetDateTime.from(((LocalDate) value).atStartOfDay()
                    .atZone(ZoneId.of("UTC")));
        }

        if (value instanceof OffsetDateTime && clazz.isAssignableFrom(Date.class)) {
            value = Date.from(((OffsetDateTime) value).toInstant());
        }

        if (value instanceof OffsetDateTime && clazz.isAssignableFrom(Calendar.class)) {
            value = GregorianCalendar.from(((OffsetDateTime) value).toZonedDateTime());
        }
        return value;
    }

    public Predicate start(Node node) {
        predicates = new ArrayList<>();
        node.accept(this);
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void visit(ConstraintNode node) {
        Path<?> path = findPath(getMappedField(node.getSelector()));

        Class<? extends Comparable> clazz = (Class<? extends Comparable>) path.getJavaType();
        Object argument = node.getArgument();

        Predicate pred;
        switch (argument) {
            case Comparable<?> comparable -> {
                Comparable<?> value = comparable;

                value = convertValue(value, clazz);

                pred = isCollectionSizeCheck(path, value)
                        ? doBuildCollectionSizePredicate(node.getComparison(), path, (Integer) value)
                        : doBuildPredicate(node.getComparison(), path.as(clazz), value);
            }
            case List ignored -> {
                //convert all values to the supported data-type
                List<Comparable<?>> transformedValues =
                        ((List<Comparable<?>>) argument).stream()
                                                        .map(v -> convertValue(v, clazz))
                                                        .collect(Collectors.toList());
                pred = doBuildPredicate(node.getComparison(), path.as(clazz), sanitizeToComparable(transformedValues));
            }
            case null -> pred = doBuildPredicate(node.getComparison(), path.as(clazz), (Comparable<?>) null);
            default -> throw new IllegalArgumentException(
                    "Unable to handle argument of type " + argument.getClass().getName());
        }

        if (pred != null) {
            predicates.add(pred);
        } else {
            throw new IllegalArgumentException("Constraint: " + node + " does not resolve to a predicate");
        }

    }

    public void visit(OperationNode node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);

        Predicate pred;
        Predicate leftHandSide = predicates.get(predicates.size() - 2);
        Predicate rightHandSide = predicates.get(predicates.size() - 1);
        pred = switch (node.getOperator()) {
            case AND -> criteriaBuilder.and(leftHandSide, rightHandSide);
            case OR -> criteriaBuilder.or(leftHandSide, rightHandSide);
            case NAND -> criteriaBuilder.or(criteriaBuilder.not(leftHandSide), criteriaBuilder.not(rightHandSide));
            case NOR -> criteriaBuilder.and(criteriaBuilder.not(leftHandSide), criteriaBuilder.not(rightHandSide));
            default ->
                    throw new IllegalArgumentException("OperationNode: " + node + " does not resolve to a operation");
        };

        predicates.remove(leftHandSide);
        predicates.remove(rightHandSide);
        predicates.add(pred);
    }

}
