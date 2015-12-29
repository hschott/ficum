package org.ficum.visitor;

import static org.junit.Assert.assertEquals;

import org.ficum.visitor.SQLEscapeUtils;
import org.junit.Test;

public class SQLEscapeUtilsTest {

    @Test
    public void testAll() {
        assertEquals("%", SQLEscapeUtils.escapeAndConvertWildcards("*", false));
    }

    @Test
    public void testAlways() {
        assertEquals("%abc%", SQLEscapeUtils.escapeAndConvertWildcards("abc", true));
    }

    @Test
    public void testEscapeEscpae() {
        assertEquals("\\\\", SQLEscapeUtils.escapeAndConvertWildcards("\\", false));
    }

    @Test
    public void testEscapeSqlZeroOrMoreWildcard() {
        assertEquals("\\%", SQLEscapeUtils.escapeAndConvertWildcards("%", false));
    }

    @Test
    public void testEscapeSqlZeroOrOneWildcard() {
        assertEquals("\\_", SQLEscapeUtils.escapeAndConvertWildcards("_", false));
    }

    @Test
    public void testLeading() {
        assertEquals("%abc", SQLEscapeUtils.escapeAndConvertWildcards("*abc", false));
    }

    @Test
    public void testLeadingTrailing() {
        assertEquals("%abc%", SQLEscapeUtils.escapeAndConvertWildcards("*abc*", false));
    }

    @Test
    public void testNoChange() {
        assertEquals("abc", SQLEscapeUtils.escapeAndConvertWildcards("abc", false));
    }

    @Test
    public void testSingle() {
        assertEquals("a_c", SQLEscapeUtils.escapeAndConvertWildcards("a?c", false));
    }

    @Test
    public void testTrailing() {
        assertEquals("abc%", SQLEscapeUtils.escapeAndConvertWildcards("abc*", false));
    }
}