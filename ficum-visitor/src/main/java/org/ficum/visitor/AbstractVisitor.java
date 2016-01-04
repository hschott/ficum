package org.ficum.visitor;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.ficum.node.AndNode;
import org.ficum.node.ConstraintNode;
import org.ficum.node.Node;
import org.ficum.node.OrNode;
import org.ficum.node.Visitor;

public abstract class AbstractVisitor<T> implements Visitor<T> {

    private boolean alwaysWildcard = false;

    private boolean distinct = true;

    private Set<Class<?>> mappedTypes = new HashSet<Class<?>>();

    public AbstractVisitor() {
        super();
        mappedTypes.add(String.class);
        mappedTypes.add(Character.class);
        mappedTypes.add(Boolean.class);
        mappedTypes.add(Number.class);
        mappedTypes.add(Date.class);
        mappedTypes.add(Calendar.class);
        mappedTypes.add(Enum.class);
    }

    public static boolean containsWildcard(String value) {
        return value.contains("*") || value.contains("?");
    }

    public void addMappedType(Class<?> mappedType) {
        mappedTypes.add(mappedType);
    }

    protected Field getField(Class<?> clazz, String name) {
        Field field = FieldUtils.getField(clazz, name, true);
        if (field == null) {
            throw new IllegalArgumentException(String.format("Can not find field %s in %s", name, clazz.getName()));
        }
        return field;
    }

    protected Class<?> getGenericTypeClazz(ParameterizedType type) {
        Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(type);
        Iterator<Type> it = typeArguments.values().iterator();
        if (it.hasNext()) {
            return TypeUtils.getRawType(it.next(), null);
        }
        return Object.class;
    }

    public boolean isAlwaysWildcard() {
        return alwaysWildcard;
    }

    protected boolean isCollection(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
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

    public void setAlwaysWildcard(boolean alwaysWildcardMatch) {
        this.alwaysWildcard = alwaysWildcardMatch;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public void visit(Node node) {
        if (node instanceof ConstraintNode) {
            visit((ConstraintNode) node);
            return;
        }
        if (node instanceof AndNode) {
            visit((AndNode) node);
            return;
        }
        if (node instanceof OrNode) {
            visit((OrNode) node);
            return;
        }
    }

}
