package de.bitgrip.ficum.visitor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WildcardsTest {

    @Test
    public void testRegexAll() {
        assertEquals("^.*$", Wildcards.escapeAndConvertToRegexWildcards("*", false));
    }

    @Test
    public void testRegexAlways() {
        assertEquals(".*abc.*", Wildcards.escapeAndConvertToRegexWildcards("abc", true));
    }

    @Test
    public void testRegexEscapeReserved() {
        assertEquals("^a\\(b\\)c\\{d\\}e\\+f\\.g\\^h\\$i\\|j\\[k\\]l\\\\m$",
                Wildcards.escapeAndConvertToRegexWildcards("a(b)c{d}e+f.g^h$i|j[k]l\\m", false));
    }

    @Test
    public void testRegexEscapeZeroOrMoreWildcard() {
        assertEquals("^\\..*$", Wildcards.escapeAndConvertToRegexWildcards(".*", false));
    }

    @Test
    public void testRegexEscapeZeroOrOneWildcard() {
        assertEquals("^\\..?$", Wildcards.escapeAndConvertToRegexWildcards(".?", false));
    }

    @Test
    public void testRegexLeading() {
        assertEquals("^.*abc$", Wildcards.escapeAndConvertToRegexWildcards("*abc", false));
    }

    @Test
    public void testRegexLeadingTrailing() {
        assertEquals("^.*abc.*$", Wildcards.escapeAndConvertToRegexWildcards("*abc*", false));
    }

    @Test
    public void testRegexNoChange() {
        assertEquals("^abc$", Wildcards.escapeAndConvertToRegexWildcards("abc", false));
    }

    @Test
    public void testRegexSingle() {
        assertEquals("^a.?c$", Wildcards.escapeAndConvertToRegexWildcards("a?c", false));
    }

    @Test
    public void testRegexTrailing() {
        assertEquals("^abc.*$", Wildcards.escapeAndConvertToRegexWildcards("abc*", false));
    }

    @Test
    public void testSQLAll() {
        assertEquals("%", Wildcards.escapeAndConvertToSQLWildcards("*", false));
    }

    @Test
    public void testSQLAlways() {
        assertEquals("%abc%", Wildcards.escapeAndConvertToSQLWildcards("abc", true));
    }

    @Test
    public void testSQLEscapeEscape() {
        assertEquals("\\\\", Wildcards.escapeAndConvertToSQLWildcards("\\", false));
    }

    @Test
    public void testSQLEscapeZeroOrMoreWildcard() {
        assertEquals("\\%", Wildcards.escapeAndConvertToSQLWildcards("%", false));
    }

    @Test
    public void testSQLEscapeZeroOrOneWildcard() {
        assertEquals("\\_", Wildcards.escapeAndConvertToSQLWildcards("_", false));
    }

    @Test
    public void testSQLLeading() {
        assertEquals("%abc", Wildcards.escapeAndConvertToSQLWildcards("*abc", false));
    }

    @Test
    public void testSQLLeadingTrailing() {
        assertEquals("%abc%", Wildcards.escapeAndConvertToSQLWildcards("*abc*", false));
    }

    @Test
    public void testSQLNoChange() {
        assertEquals("abc", Wildcards.escapeAndConvertToSQLWildcards("abc", false));
    }

    @Test
    public void testSQLSingle() {
        assertEquals("a_c", Wildcards.escapeAndConvertToSQLWildcards("a?c", false));
    }

    @Test
    public void testSQLTrailing() {
        assertEquals("abc%", Wildcards.escapeAndConvertToSQLWildcards("abc*", false));
    }
}
