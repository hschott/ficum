package org.ficum.parser;

import java.util.ArrayDeque;
import java.util.Deque;

import org.ficum.node.Comparison;
import org.ficum.node.Constraint;
import org.ficum.node.Operator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

public class ExpressionParserTest {

    private TracingParseRunner<Deque<Object>> parseRunner;

    private void assertError(Class<?> expected, String input) {
        ParsingResult<Deque<Object>> result = parseRunner.run(input);
        logInfo(result);
        Assert.assertTrue(result.hasErrors());
        Assert.assertEquals(1, result.parseErrors.size());
        Assert.assertTrue(result.parseErrors.get(0).getClass().isAssignableFrom(expected));
    }

    private void assertValue(Deque<?> expected, String input) {
        ParsingResult<Deque<Object>> result = parseRunner.run(input);
        logInfo(result);
        Assert.assertFalse(ErrorUtils.printParseErrors(result.parseErrors), result.hasErrors());
        Assert.assertTrue(result.matched);

        Deque<Object> actual = result.resultValue;

        Assert.assertArrayEquals(expected.toArray(), actual.toArray());
    }

    void logInfo(ParsingResult<Deque<Object>> result) {
        if (result.hasErrors()) {
            System.out.println(ErrorUtils.printParseErrors(result.parseErrors));
        } else if (result.matched) {
            System.out.println("NodeTree: " + ParseTreeUtils.printNodeTree(result) + '\n');
        }
    }

    @Before
    public void setUp() {
        String[] allowedPaths = { "first", "second", "third", "fourth", "fifth" };
        ConstraintParser parser = Parboiled.createParser(ExpressionParser.class, (Object) allowedPaths);

        parseRunner = new TracingParseRunner<Deque<Object>>(parser.root());
    }

    @Test()
    public void testAndOperator() {
        String input = "first=gt=1l,second=le=2l,third=gt=3l";

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.addLast(new Constraint("first", Comparison.GREATER_THAN, 1L));
        expected.addLast(Operator.AND);
        expected.addLast(new Constraint("second", Comparison.LESS_EQUALS, 2L));
        expected.addLast(Operator.AND);
        expected.addLast(new Constraint("third", Comparison.GREATER_THAN, 3L));

        assertValue(expected, input);
    }

    @Test()
    public void testNestedPrecededOperator() {
        String input = "first=gt=1l,(second=le=2l;(third=gt=3;fourth==4f),fifth=lt='five')";

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.addLast(new Constraint("first", Comparison.GREATER_THAN, 1L));
        expected.addLast(Operator.AND);
        expected.addLast(Operator.LEFT);
        expected.addLast(new Constraint("second", Comparison.LESS_EQUALS, 2L));
        expected.addLast(Operator.OR);
        expected.addLast(Operator.LEFT);
        expected.addLast(new Constraint("third", Comparison.GREATER_THAN, 3));
        expected.addLast(Operator.OR);
        expected.addLast(new Constraint("fourth", Comparison.EQUALS, 4f));
        expected.addLast(Operator.RIGHT);
        expected.addLast(Operator.AND);
        expected.addLast(new Constraint("fifth", Comparison.LESS_THAN, "five"));
        expected.addLast(Operator.RIGHT);

        assertValue(expected, input);
    }

    @Test()
    public void testOrBeforeAndOperator() {
        String input = "first==true;second=le=2l,third=gt=3f";

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.addLast(new Constraint("first", Comparison.EQUALS, true));
        expected.addLast(Operator.OR);
        expected.addLast(new Constraint("second", Comparison.LESS_EQUALS, 2L));
        expected.addLast(Operator.AND);
        expected.addLast(new Constraint("third", Comparison.GREATER_THAN, 3f));

        assertValue(expected, input);
    }

    @Test()
    public void testOrOperator() {
        String input = "first=gt=1L;second=le=2F;third=gt=3";

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.addLast(new Constraint("first", Comparison.GREATER_THAN, 1L));
        expected.addLast(Operator.OR);
        expected.addLast(new Constraint("second", Comparison.LESS_EQUALS, 2F));
        expected.addLast(Operator.OR);
        expected.addLast(new Constraint("third", Comparison.GREATER_THAN, 3));

        assertValue(expected, input);
    }

    @Test()
    public void testOrPrecededOperator() {
        String input = "first=gt=1,(second=le=2;third=gt=3)";

        Deque<Object> expected = new ArrayDeque<Object>();

        expected.addLast(new Constraint("first", Comparison.GREATER_THAN, 1));
        expected.addLast(Operator.AND);
        expected.addLast(Operator.LEFT);
        expected.addLast(new Constraint("second", Comparison.LESS_EQUALS, 2));
        expected.addLast(Operator.OR);
        expected.addLast(new Constraint("third", Comparison.GREATER_THAN, 3));
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
        expected.addLast(new Constraint("first", Comparison.GREATER_THAN, -1F));
        expected.addLast(Operator.RIGHT);

        assertValue(expected, input);
    }

}
