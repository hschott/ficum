package org.hschott.ficum.spring;

import org.hschott.ficum.annotation.FicumExpression;
import org.hschott.ficum.node.Node;
import org.hschott.ficum.parser.ParseHelper;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

import java.util.Collections;
import java.util.Set;

/**
 * Converts from a String to an Ficum Node
 */
public class StringToFicumNodeConverter implements ConditionalGenericConverter {

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return String.class.isAssignableFrom(sourceType.getType())
                && Node.class.isAssignableFrom(targetType.getType())
                && targetType.hasAnnotation(FicumExpression.class);
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Node.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        FicumExpression ficumExpressionAnnotation = targetType.getAnnotation(FicumExpression.class);
        return ParseHelper.parse((String) source, ficumExpressionAnnotation.value());
    }
}
