package org.hschott.ficum.spring;

import org.hschott.ficum.annotation.FicumExpression;
import org.hschott.ficum.node.Builder;
import org.hschott.ficum.node.Comparison;
import org.hschott.ficum.node.Node;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.GenericConversionService;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.Assert.*;


public class StringToFicumNodeConverterTest {
    private StringToFicumNodeConverter converter;
    private GenericConversionService conversionService;

    @FicumExpression({"foo"})
    private Node node;

    @Before
    public void setUp() {
        converter = new StringToFicumNodeConverter();
        conversionService = new GenericConversionService();
        conversionService.addConverter(converter);
    }

    @Test
    public void testGetConvertibleTypes() {
        Set<?> convertibleTypes = converter.getConvertibleTypes();

        assertNotNull(convertibleTypes);
        assertEquals(1, convertibleTypes.size());
        assertTrue(convertibleTypes.stream().anyMatch(
                type -> type.equals(new GenericConverter.ConvertiblePair(String.class, Node.class))));
    }

    @Test
    public void testMatches() throws NoSuchFieldException {
        Field field = getClass().getDeclaredField("node");
        TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
        TypeDescriptor targetType = new TypeDescriptor(field);

        assertTrue(converter.matches(sourceType, targetType));
    }

    @Test
    public void testConvertValidInput() throws NoSuchFieldException {
        Field field = getClass().getDeclaredField("node");
        TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
        TypeDescriptor targetType = new TypeDescriptor(field);

        Object result = converter.convert("foo=='bar'",
                                          sourceType, targetType);

        assertNotNull(result);
        assertTrue(result instanceof Node);
        assertEquals(Builder.start()
                            .constraint("foo", Comparison.EQUALS, "bar")
                            .build()
                , result);
    }

    @Test
    public void testConvertNullInput() throws NoSuchFieldException {
        Field field = getClass().getDeclaredField("node");
        TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
        TypeDescriptor targetType = new TypeDescriptor(field);

        Object result = converter.convert(null, sourceType, targetType);
        assertNull(result);
    }

    @Test
    public void testConversionWithConversionService() throws NoSuchFieldException {
        Field field = getClass().getDeclaredField("node");
        TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
        TypeDescriptor targetType = new TypeDescriptor(field);

        Object result = conversionService.convert(
                "foo=='bar'", sourceType, targetType);

        assertNotNull(result);
        assertTrue(result instanceof Node);
        assertEquals(Builder.start()
                            .constraint("foo", Comparison.EQUALS, "bar")
                            .build(), result);
    }

}