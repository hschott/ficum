package org.ficum.visitor;

public class SQLEscapeUtils {

    public static final char ESCAPE_CHAR = '\\';

    public static boolean containsEscapedChar(String value) {
        return value.contains("\\%") || value.contains("\\\\") || value.contains("\\_");
    }

    public static String escapeAndConvertWildcards(String value, boolean alwaysWildcard) {
        String ret = value.replaceAll("\\\\", "\\\\\\\\") // escape 'sql escape'
                                                          // char
                .replaceAll("_", "\\\\_").replaceAll("%", "\\\\%") // escape sql
                                                                   // wildcards
                .replaceAll("\\*", "%").replaceAll("\\?", "_"); // replace rql
                                                                // wildcard with
                                                                // sql wildcard

        return alwaysWildcard ? "%" + ret + "%" : ret;
    }

}
