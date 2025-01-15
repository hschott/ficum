package org.hschott.ficum.spring;

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


public class FicumNodeToStringConverterTest {
    private FicumNodeToStringConverter converter;
    private GenericConversionService conversionService;

    private String query;

    @Before
    public void setUp() {
        converter = new FicumNodeToStringConverter();
        conversionService = new GenericConversionService();
        conversionService.addConverter(converter);
    }

    @Test
    public void testGetConvertibleTypes() {
        Set<?> convertibleTypes = converter.getConvertibleTypes();

        assertNotNull(convertibleTypes);
        assertEquals(1, convertibleTypes.size());
        assertTrue(convertibleTypes.stream().anyMatch(
                type -> type.equals(new GenericConverter.ConvertiblePair(Node.class, String.class))));
    }

    @Test
    public void testMatches() throws NoSuchFieldException {
        Field field = getClass().getDeclaredField("query");
        TypeDescriptor sourceType = TypeDescriptor.valueOf(Node.class);
        TypeDescriptor targetType = new TypeDescriptor(field);

        assertTrue(converter.matches(sourceType, targetType));
    }

    @Test
    public void testConvertValidInput() throws NoSuchFieldException {
        Field field = getClass().getDeclaredField("query");
        TypeDescriptor sourceType = TypeDescriptor.valueOf(Node.class);
        TypeDescriptor targetType = new TypeDescriptor(field);

        Object result = converter.convert(Builder.start().constraint("foo", Comparison.EQUALS, "bar").build(),
                                          sourceType, targetType);

        assertNotNull(result);
        assertTrue(result instanceof String);
        assertEquals("foo=='bar'", result);
    }

    @Test
    public void testConvertNullInput() throws NoSuchFieldException {
        Field field = getClass().getDeclaredField("query");
        TypeDescriptor sourceType = TypeDescriptor.valueOf(Node.class);
        TypeDescriptor targetType = new TypeDescriptor(field);

        Object result = converter.convert(null, sourceType, targetType);

        assertNull(result);
    }

    @Test
    public void testConversionWithConversionService() throws NoSuchFieldException {
        Field field = getClass().getDeclaredField("query");
        TypeDescriptor sourceType = TypeDescriptor.valueOf(Node.class);
        TypeDescriptor targetType = new TypeDescriptor(field);

        Object result = conversionService.convert(
                Builder.start().constraint("foo", Comparison.EQUALS, "bar").build(), sourceType, targetType);

        assertNotNull(result);
        assertTrue(result instanceof String);
        assertEquals("foo=='bar'", result);
    }

}