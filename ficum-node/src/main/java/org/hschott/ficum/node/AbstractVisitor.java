package org.hschott.ficum.node;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoField.*;

public abstract class AbstractVisitor<T> implements Visitor<T> {
    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .appendFraction(MILLI_OF_SECOND, 3, 3, true)
            .appendOffsetId()
            .toFormatter(Locale.ROOT);

    private boolean alwaysWildcard = false;

    private Map<Selector, String> selectorToFieldMapping = new HashMap<>();

    public static boolean containsWildcard(String value) {
        return value.contains("*") || value.contains("?");
    }

    public void addSelectorToFieldMapping(String selector, String field) {
        selectorToFieldMapping.put(new SimpleSelector(selector), field);
    }

    public String getMappedField(Selector selector) {
        return selectorToFieldMapping.getOrDefault(selector, selector.value());
    }

    public boolean isAlwaysWildcard() {
        return alwaysWildcard;
    }

    public void setAlwaysWildcard(boolean alwaysWildcardMatch) {
        this.alwaysWildcard = alwaysWildcardMatch;
    }

    public void setSelectorToFieldMapping(Map<Selector, String> selectorToFieldMapping) {
        this.selectorToFieldMapping = selectorToFieldMapping;
    }

    protected List<Comparable> sanitizeToComparable(List<?> arguments) {
        return arguments.stream().filter(Comparable.class::isInstance).map(Comparable.class::cast).collect(Collectors.toList());
    }
}
