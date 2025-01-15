package org.hschott.ficum.spring;

import org.hschott.ficum.node.Node;
import org.hschott.ficum.node.QueryPrinterVisitor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

import java.util.Collections;
import java.util.Set;

/**
 * Converts from a Ficum Node to an String
 */
public class FicumNodeToStringConverter implements ConditionalGenericConverter {

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return Node.class.isAssignableFrom(sourceType.getType()) && String.class.isAssignableFrom(targetType.getType());
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Node.class, String.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        QueryPrinterVisitor queryPrinterVisitor = new QueryPrinterVisitor();
        return queryPrinterVisitor.start((Node) source);
    }
}
