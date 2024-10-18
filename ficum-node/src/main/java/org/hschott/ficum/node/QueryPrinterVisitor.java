package org.hschott.ficum.node;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A Visitor that prints the Node tree as FICUM query dsl.
 */
public class QueryPrinterVisitor extends AbstractVisitor<String> {

    private StringBuffer output;
    private boolean preceded = false;

    private String print(Object argument) {
        return switch (argument) {
            case null -> "null";
            case Boolean b -> b.toString();
            case Byte b -> b.toString();
            case Short s -> s.toString();
            case Integer i -> i.toString();
            case Float f -> f.toString().concat("f");
            case Long l -> l.toString().concat("L");
            case Double d -> d.toString();
            case UUID u -> u.toString();
            case Date date -> ISO_OFFSET_DATE_TIME.format(date.toInstant().atZone(ZoneOffset.systemDefault()));
            case Calendar c ->
                    ISO_OFFSET_DATE_TIME.format(OffsetDateTime.ofInstant(Instant.ofEpochMilli(c.getTimeInMillis()), c.getTimeZone().toZoneId()));
            case LocalDate l -> DateTimeFormatter.ISO_LOCAL_DATE.format(l);
            case LocalDateTime l -> ISO_OFFSET_DATE_TIME.format(l.atZone(ZoneOffset.systemDefault()));
            case OffsetDateTime o -> ISO_OFFSET_DATE_TIME.format(o);
            case ZonedDateTime z -> ISO_OFFSET_DATE_TIME.format(z);
            case Enum<?> e -> "\'".concat(e.name()).concat("\'");
            case Iterable<?> i ->
                    "[".concat(StreamSupport.stream(i.spliterator(), false).map(this::print).collect(Collectors.joining(","))).concat("]");

            default -> "\'".concat(argument.toString()).concat("\'");
        };
    }

    public String start(Node node) {
        output = new StringBuffer();
        node.accept(this);
        return output.toString();
    }

    public void visit(ConstraintNode<?> node) {
        output.append(node.getSelector().value());
        output.append(node.getComparison().getSign());
        output.append(print(node.getArgument()));
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
                if (preceded) output.append('(');
                node.getLeft().accept(this);
                output.append(node.getOperator().getSign());
                node.getRight().accept(this);
                if (preceded) output.append(')');

                break;

            default:
                throw new IllegalArgumentException("OperationNode: " + node + " does not resolve to a operation");
        }

    }

}
