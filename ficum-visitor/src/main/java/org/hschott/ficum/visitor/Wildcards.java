package org.hschott.ficum.visitor;

import java.util.regex.Pattern;

public class Wildcards {

    private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+^$\\\\|]");

    private Wildcards() {
        // static helper
    }

    public static String escapeAndConvertToRegexWildcards(String value, boolean alwaysWildcard) {
        String ret = SPECIAL_REGEX_CHARS.matcher(value).replaceAll("\\\\$0").replaceAll("\\*", ".*").replaceAll("\\?",
                ".?");
        return alwaysWildcard ? ".*" + ret + ".*" : "^" + ret + "$";
    }

    public static String escapeAndConvertToSQLWildcards(String value, boolean alwaysWildcard) {
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
