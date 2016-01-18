package org.ficum.visitor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MongoDBWildcardTest {

    @Test
    public void testAll() {
        assertEquals(".*", MongoDBFilterVisitor.escapeAndConvertWildcards("*", false));
    }

    @Test
    public void testAlways() {
        assertEquals(".*abc.*", MongoDBFilterVisitor.escapeAndConvertWildcards("abc", true));
    }

    @Test
    public void testEscapeReserved() {
        assertEquals("a\\(b\\)c\\{d\\}e\\+f\\.g\\^h\\$i\\|j\\[k\\]l\\\\m",
                MongoDBFilterVisitor.escapeAndConvertWildcards("a(b)c{d}e+f.g^h$i|j[k]l\\m", false));
    }

    @Test
    public void testEscapeSqlZeroOrMoreWildcard() {
        assertEquals("\\..*", MongoDBFilterVisitor.escapeAndConvertWildcards(".*", false));
    }

    @Test
    public void testEscapeSqlZeroOrOneWildcard() {
        assertEquals("\\..?", MongoDBFilterVisitor.escapeAndConvertWildcards(".?", false));
    }

    @Test
    public void testLeading() {
        assertEquals(".*abc", MongoDBFilterVisitor.escapeAndConvertWildcards("*abc", false));
    }

    @Test
    public void testLeadingTrailing() {
        assertEquals(".*abc.*", MongoDBFilterVisitor.escapeAndConvertWildcards("*abc*", false));
    }

    @Test
    public void testNoChange() {
        assertEquals("abc", MongoDBFilterVisitor.escapeAndConvertWildcards("abc", false));
    }

    @Test
    public void testSingle() {
        assertEquals("a.?c", MongoDBFilterVisitor.escapeAndConvertWildcards("a?c", false));
    }

    @Test
    public void testTrailing() {
        assertEquals("abc.*", MongoDBFilterVisitor.escapeAndConvertWildcards("abc*", false));
    }
}