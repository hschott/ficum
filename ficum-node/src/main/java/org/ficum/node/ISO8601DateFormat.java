package org.ficum.node;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public abstract class ISO8601DateFormat {

    /** ISO8601 timestamp format */
    public static final DateTimeFormatter ISO8601_TIMESTAMP = ISODateTimeFormat.dateTime();
    public static final DateTimeFormatter ISO8601_DATE = ISODateTimeFormat.yearMonthDay();

}
