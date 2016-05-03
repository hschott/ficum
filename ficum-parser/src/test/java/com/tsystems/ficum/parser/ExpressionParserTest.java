package com.tsystems.ficum.parser;

import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.common.StringBuilderSink;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.errors.ParseError;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsystems.ficum.node.Comparison;
import com.tsystems.ficum.node.Constraint;
import com.tsystems.ficum.node.Operator;

public class ExpressionParserTest {
    private static final Logger LOG = LoggerFactory.getLogger(ExpressionParserTest.class);

    private TracingParseRunner<Deque<Object>> parseRunner;

    private StringBuilderSink sink;

    private void assertError(Class<?> expected, String input) {
        ParsingResult<Deque<Object>> result = parseRunner.run(input);
        logInfo(result);
        Assert.assertTrue(result.hasErrors());
        for (ParseError parseError : result.parseErrors) {
            Assert.assertTrue(parseError.getClass().isAssignableFrom(expected));
        }
    }

    private void assertValue(Deque<?> expected, String input) {
        ParsingResult<Deque<Object>> result = parseRunner.run(input);
        logInfo(result);
        Assert.assertFalse(ErrorUtils.printParseErrors(result.parseErrors), result.hasErrors());
        Assert.assertTrue(result.matched);

        Deque<Object> actual = result.resultValue;

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    private void logInfo(ParsingResult<Deque<Object>> result) {
        LOG.info(sink.toString());
        if (result.hasErrors()) {
            LOG.info(ErrorUtils.printParseErrors(result.parseErrors));
        } else if (result.matched) {
            LOG.info("NodeTree: " + ParseTreeUtils.printNodeTree(result) + '\n');
        }
    }

    @Before
    public void setUp() {
        String[] allowedPaths = { "first", "second", "third", "fourth", "fifth" };
        ConstraintParser parser = Parboiled.createParser(ExpressionParser.class, (Object) allowedPaths);

        sink = new StringBuilderSink();
        parseRunner = new TracingParseRunner<Deque<Object>>(parser.root()).withLog(sink);
    }

    @Test()
    public void testAndOperator() {
        String input = "first=gt=1l,second=le=2l,third=gt=3l";

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.addLast(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        expected.addLast(Operator.AND);
        expected.addLast(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        expected.addLast(Operator.AND);
        expected.addLast(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3L));

        assertValue(expected, input);
    }

    @Test()
    public void testNestedPrecededOperator() {
        String input = "first=gt=1l,(second=le=2l;(third=gt=3;fourth==4f),fifth=lt='five')";

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.addLast(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        expected.addLast(Operator.AND);
        expected.addLast(Operator.LEFT);
        expected.addLast(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        expected.addLast(Operator.OR);
        expected.addLast(Operator.LEFT);
        expected.addLast(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3));
        expected.addLast(Operator.OR);
        expected.addLast(new Constraint<Comparable<?>>("fourth", Comparison.EQUALS, 4f));
        expected.addLast(Operator.RIGHT);
        expected.addLast(Operator.AND);
        expected.addLast(new Constraint<Comparable<?>>("fifth", Comparison.LESS_THAN, "five"));
        expected.addLast(Operator.RIGHT);

        assertValue(expected, input);
    }

    @Test()
    public void testOrBeforeAndOperator() {
        String input = "first==true;second=le=2l,third=gt=3.34f";

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.addLast(new Constraint<Comparable<?>>("first", Comparison.EQUALS, true));
        expected.addLast(Operator.OR);
        expected.addLast(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2L));
        expected.addLast(Operator.AND);
        expected.addLast(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3.34f));

        assertValue(expected, input);
    }

    @Test()
    public void testOrOperator() {
        String input = "first=gt=1L;second=le=2F;third=gt=3";

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.addLast(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1L));
        expected.addLast(Operator.OR);
        expected.addLast(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2F));
        expected.addLast(Operator.OR);
        expected.addLast(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3));

        assertValue(expected, input);
    }

    @Test()
    public void testOrPrecededOperator() {
        String input = "first=gt=1,(second=le=2;third=gt=3)";

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.addLast(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, 1));
        expected.addLast(Operator.AND);
        expected.addLast(Operator.LEFT);
        expected.addLast(new Constraint<Comparable<?>>("second", Comparison.LESS_EQUALS, 2));
        expected.addLast(Operator.OR);
        expected.addLast(new Constraint<Comparable<?>>("third", Comparison.GREATER_THAN, 3));
        expected.addLast(Operator.RIGHT);

        assertValue(expected, input);
    }

    @Test()
    public void testPrecededNotClosedOperator() {
        String input = "(first=gt=1";

        assertError(InvalidInputError.class, input);
    }

    @Test()
    public void testPrecededNotOpenedOperator() {
        String input = "first=gt=1)";

        assertError(InvalidInputError.class, input);
    }

    @Test()
    public void testPrecededOperator() {
        String input = "(first=gt=-1f)";

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.addLast(Operator.LEFT);
        expected.addLast(new Constraint<Comparable<?>>("first", Comparison.GREATER_THAN, -1F));
        expected.addLast(Operator.RIGHT);

        assertValue(expected, input);
    }

}
