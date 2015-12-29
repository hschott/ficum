package org.ficum.visitor;

import java.util.Calendar;
import java.util.Date;

import org.ficum.node.AndNode;
import org.ficum.node.ConstraintNode;
import org.ficum.node.ISO8601DateFormat;
import org.ficum.node.Node;
import org.ficum.node.OrNode;
import org.joda.time.DateTime;

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
        }
        if (argument instanceof Integer) {
            output.append(argument);
        }
        if (argument instanceof Float) {
            output.append(argument);
        }
        if (argument instanceof Long) {
            output.append(argument).append("l");
        }
        if (argument instanceof Double) {
            output.append(argument).append("d");
        }
        if (argument instanceof Date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) argument);
            if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0 && cal.get(Calendar.SECOND) == 0
                    && cal.get(Calendar.MILLISECOND) == 0) {
                output.append(ISO8601DateFormat.ISO8601_DATE.print(new DateTime(argument)));
            } else {
                output.append(ISO8601DateFormat.ISO8601_TIMESTAMP.print(new DateTime(argument)));
            }
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
