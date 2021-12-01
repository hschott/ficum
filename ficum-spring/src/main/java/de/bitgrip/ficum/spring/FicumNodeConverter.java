package de.bitgrip.ficum.spring;

import de.bitgrip.ficum.annotation.FicumExpression;
import de.bitgrip.ficum.node.Node;
import de.bitgrip.ficum.node.QueryPrinterVisitor;
import de.bitgrip.ficum.parser.ParseHelper;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.Set;

/**
 * <p>Generic Converter which is able to Convert a String to a Ficum Node</p>
 * <p>
 * (w) marcelmuller
 * (c) bitgrip GmbH, 2019
 * </p>
 */
public class FicumNodeConverter implements GenericConverter {

    @Override
    public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
        return Set.of(new ConvertiblePair(String.class, Node.class), new ConvertiblePair(Node.class, String.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (sourceType.getType().equals(String.class)) {
            FicumExpression ficumExpressionAnnotation = targetType.getAnnotation(FicumExpression.class);
            if (ficumExpressionAnnotation == null || ficumExpressionAnnotation.value().length == 0) {
                throw new IllegalStateException("missing " + FicumExpression.class.getName() + " annotation to node parameter" +
                        " for defining allowedSelectorNames");
            }
            return convertStringToNode((String) source, ficumExpressionAnnotation.value());
        } else if (Node.class.isAssignableFrom(sourceType.getType())) {
            return convertNodeToString((Node) source);
        }

        throw new IllegalArgumentException("Invalid source type " + source.getClass().getCanonicalName());
    }

    private Node convertStringToNode(String ficumQuery, String[] allowedSelectorNames) {
        return ParseHelper.parse(ficumQuery, allowedSelectorNames);
    }

    private String convertNodeToString(Node filter) {
        QueryPrinterVisitor queryPrinterVisitor = new QueryPrinterVisitor();
        return queryPrinterVisitor.start(filter);
    }
}
