package org.ficum.visitor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MongoDBWildcardTest {

    @Test
    public void testAll() {
        assertEquals(".*", MongoDBVisitor.escapeAndConvertWildcards("*", false));
    }

    @Test
    public void testAlways() {
        assertEquals(".*abc.*", MongoDBVisitor.escapeAndConvertWildcards("abc", true));
    }

    @Test
    public void testEscapeReserved() {
        assertEquals("a\\(b\\)c\\{d\\}e\\+f\\.g\\^h\\$i\\|j\\[k\\]l\\\\m",
                MongoDBVisitor.escapeAndConvertWildcards("a(b)c{d}e+f.g^h$i|j[k]l\\m", false));
    }

    @Test
    public void testEscapeSqlZeroOrMoreWildcard() {
        assertEquals("\\..*", MongoDBVisitor.escapeAndConvertWildcards(".*", false));
    }

    @Test
    public void testEscapeSqlZeroOrOneWildcard() {
        assertEquals("\\..?", MongoDBVisitor.escapeAndConvertWildcards(".?", false));
    }

    @Test
    public void testLeading() {
        assertEquals(".*abc", MongoDBVisitor.escapeAndConvertWildcards("*abc", false));
    }

    @Test
    public void testLeadingTrailing() {
        assertEquals(".*abc.*", MongoDBVisitor.escapeAndConvertWildcards("*abc*", false));
    }

    @Test
    public void testNoChange() {
        assertEquals("abc", MongoDBVisitor.escapeAndConvertWildcards("abc", false));
    }

    @Test
    public void testSingle() {
        assertEquals("a.?c", MongoDBVisitor.escapeAndConvertWildcards("a?c", false));
    }

    @Test
    public void testTrailing() {
        assertEquals("abc.*", MongoDBVisitor.escapeAndConvertWildcards("abc*", false));
    }
}