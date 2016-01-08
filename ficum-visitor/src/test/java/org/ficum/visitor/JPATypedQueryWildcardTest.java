package org.ficum.visitor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JPATypedQueryWildcardTest {

    @Test
    public void testAll() {
        assertEquals("%", JPATypedQueryVisitor.escapeAndConvertWildcards("*", false));
    }

    @Test
    public void testAlways() {
        assertEquals("%abc%", JPATypedQueryVisitor.escapeAndConvertWildcards("abc", true));
    }

    @Test
    public void testEscapeEscpae() {
        assertEquals("\\\\", JPATypedQueryVisitor.escapeAndConvertWildcards("\\", false));
    }

    @Test
    public void testEscapeSqlZeroOrMoreWildcard() {
        assertEquals("\\%", JPATypedQueryVisitor.escapeAndConvertWildcards("%", false));
    }

    @Test
    public void testEscapeSqlZeroOrOneWildcard() {
        assertEquals("\\_", JPATypedQueryVisitor.escapeAndConvertWildcards("_", false));
    }

    @Test
    public void testLeading() {
        assertEquals("%abc", JPATypedQueryVisitor.escapeAndConvertWildcards("*abc", false));
    }

    @Test
    public void testLeadingTrailing() {
        assertEquals("%abc%", JPATypedQueryVisitor.escapeAndConvertWildcards("*abc*", false));
    }

    @Test
    public void testNoChange() {
        assertEquals("abc", JPATypedQueryVisitor.escapeAndConvertWildcards("abc", false));
    }

    @Test
    public void testSingle() {
        assertEquals("a_c", JPATypedQueryVisitor.escapeAndConvertWildcards("a?c", false));
    }

    @Test
    public void testTrailing() {
        assertEquals("abc%", JPATypedQueryVisitor.escapeAndConvertWildcards("abc*", false));
    }
}