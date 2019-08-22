package de.bitgrip.ficum.node;

import org.joda.time.*;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

/**
 * A Visitor that prints the Node tree as FICUM query dsl.
 */
public class QueryPrinterVisitor extends AbstractVisitor<String> {

    private StringBuffer output;
    private boolean preceded = false;

    private void printArgument(StringBuffer buffer, Object argument) {
        if (argument == null) {
            buffer.append("null");

        } else if (argument instanceof Boolean) {
            buffer.append(argument);

        } else if (argument instanceof Byte) {
            buffer.append(argument);

        } else if (argument instanceof Short) {
            buffer.append(argument);

        } else if (argument instanceof Integer) {
            buffer.append(argument);

        } else if (argument instanceof Float) {
            buffer.append(argument).append('f');

        } else if (argument instanceof Long) {
            buffer.append(argument).append('l');

        } else if (argument instanceof Double) {
            buffer.append(argument);

        } else if (argument instanceof UUID) {
            buffer.append(argument);

        } else if (argument instanceof Date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) argument);
            printCalendar(buffer, cal);

        } else if (argument instanceof Calendar) {
            Calendar cal = (Calendar) argument;
            printCalendar(buffer, cal);

        } else if (argument instanceof ReadablePartial) {
            buffer.append(ISODateTimeFormat.yearMonthDay().print((ReadablePartial) argument));

        } else if (argument instanceof ReadableInstant) {
            buffer.append(ISODateTimeFormat.dateTime().print((ReadableInstant) argument));

        } else if (argument instanceof Enum) {
            buffer.append('\'').append(((Enum<?>) argument).name()).append('\'');

        } else if (argument instanceof Iterable) {
            @SuppressWarnings("unchecked")
            Iterator<Comparable<?>> it = ((Iterable<Comparable<?>>) argument).iterator();

            buffer.append('[');
            if (it.hasNext()) {
                printArgument(buffer, it.next());
                while (it.hasNext()) {
                    buffer.append(',');
                    printArgument(buffer, it.next());
                }
            }
            buffer.append(']');

        } else {
            buffer.append('\'').append(argument.toString()).append('\'');
        }
    }

    private void printCalendar(StringBuffer buffer, Calendar cal) {
        if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0 && cal.get(Calendar.SECOND) == 0
                && cal.get(Calendar.MILLISECOND) == 0) {
            printArgument(buffer, new LocalDate(cal, DateTimeZone.UTC));
        } else {
            printArgument(buffer, new DateTime(cal, DateTimeZone.forTimeZone(cal.getTimeZone())));
        }
    }

    public String start(Node node) {
        output = new StringBuffer();
        node.accept(this);
        return output.toString();
    }

    public void visit(ConstraintNode<?> node) {
        output.append(node.getSelector());
        output.append(node.getComparison().getSign());
        printArgument(output, node.getArgument());
    }

    public void visit(OperationNode node) {
        switch (node.getOperator()) {
            case AND:
            case NOR:
                preceded = node.getOperator().preceded;
                node.getLeft().accept(this);
                output.append(node.getOperator().getSign());
                node.getRight().accept(this);
                preceded = false;

                break;

            case OR:
            case NAND:
                if (preceded)
                    output.append('(');
                node.getLeft().accept(this);
                output.append(node.getOperator().getSign());
                node.getRight().accept(this);
                if (preceded)
                    output.append(')');

                break;

            default:
                throw new IllegalArgumentException("OperationNode: " + node + " does not resolve to a operation");
        }

    }

}
