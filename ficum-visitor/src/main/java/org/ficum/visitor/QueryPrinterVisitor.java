package org.ficum.visitor;

import java.util.Calendar;
import java.util.Date;

import org.ficum.node.AndNode;
import org.ficum.node.ConstraintNode;
import org.ficum.node.Node;
import org.ficum.node.OrNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.joda.time.format.ISODateTimeFormat;

/**
 * A Visitor that prints the Node tree as FICUM query dsl.
 *
 */
public class QueryPrinterVisitor extends AbstractVisitor<String> {

    StringBuilder output;
    boolean preceded = false;

    private void printArgument(StringBuilder output, Comparable<?> argument) {
        if (argument instanceof String) {
            output.append('\'').append((String) argument).append('\'');

        } else if (argument instanceof Integer) {
            output.append(argument);

        } else if (argument instanceof Float) {
            output.append(argument);

        } else if (argument instanceof Long) {
            output.append(argument).append("l");

        } else if (argument instanceof Double) {
            output.append(argument).append("d");

        } else if (argument instanceof Date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) argument);
            printCalendar(output, cal);

        } else if (argument instanceof Calendar) {
            Calendar cal = (Calendar) argument;
            printCalendar(output, cal);

        } else if (argument instanceof ReadablePartial) {
            output.append(ISODateTimeFormat.yearMonthDay().print((ReadablePartial) argument));

        } else if (argument instanceof ReadableInstant) {
            output.append(ISODateTimeFormat.dateTime().print((ReadableInstant) argument));

        } else {
            output.append(argument.toString());
        }
    }

    private void printCalendar(StringBuilder output, Calendar cal) {
        if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0 && cal.get(Calendar.SECOND) == 0
                && cal.get(Calendar.MILLISECOND) == 0) {
            output.append(ISODateTimeFormat.yearMonthDay().print(new DateTime(cal)));
        } else {
            output.append(
                    ISODateTimeFormat.dateTime().print(new DateTime(cal, DateTimeZone.forTimeZone(cal.getTimeZone()))));
        }
    }

    public String start(Node node) {
        output = new StringBuilder();
        node.accept(this);
        return output.toString();
    }

    public void visit(AndNode node) {
        preceded = true;
        node.getLeft().accept(this);
        output.append(new Character(node.getOperator().sign).toString());
        node.getRight().accept(this);
        preceded = false;
    }

    public void visit(ConstraintNode node) {
        output.append(node.getSelector());
        output.append(node.getComparison().sign);
        printArgument(output, node.getArgument());
    }

    public void visit(OrNode node) {
        if (preceded)
            output.append('(');
        node.getLeft().accept(this);
        output.append(new Character(node.getOperator().sign).toString());
        node.getRight().accept(this);
        if (preceded)
            output.append(')');
    }
}
