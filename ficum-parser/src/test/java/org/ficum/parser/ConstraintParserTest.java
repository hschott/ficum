package org.ficum.parser;

import org.ficum.node.Comparison;
import org.ficum.node.Constraint;
import org.ficum.parser.ConstraintParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

public class ConstraintParserTest {

    private TracingParseRunner<Constraint> parseRunner;

    private void assertValue(Constraint expected, String input) {
        ParsingResult<Constraint> result = parseRunner.run(input);
        logInfo(result);
        Assert.assertFalse(ErrorUtils.printParseErrors(result.parseErrors), result.hasErrors());
        Assert.assertTrue(result.matched);
        Assert.assertEquals(expected, result.resultValue);
    }

    void logInfo(ParsingResult<Constraint> result) {
        if (result.hasErrors()) {
            System.out.println(ErrorUtils.printParseErrors(result.parseErrors));
        } else if (result.matched) {
            System.out.println("NodeTree: " + ParseTreeUtils.printNodeTree(result) + '\n');
        }
    }

    @Before
    public void setUp() {
        String[] allowedPaths = { "first", "second", "third" };
        ConstraintParser parser = Parboiled.createParser(ConstraintParser.class, (Object) allowedPaths);

        parseRunner = new TracingParseRunner<Constraint>(parser.root());
    }

    @Test()
    public void testEquals() {
        Constraint expected = new Constraint("first.second", Comparison.EQUALS, 1);
        String input = "first.second==1";

        assertValue(expected, input);
    }

    @Test()
    public void testGreaterEquals() {
        Constraint expected = new Constraint("first.second", Comparison.GREATER_EQUALS, 1);
        String input = "first.second=ge=1";

        assertValue(expected, input);
    }

    @Test()
    public void testGreaterThan() {
        Constraint expected = new Constraint("first.second", Comparison.GREATER_THAN, 1);
        String input = "first.second=gt=1";

        assertValue(expected, input);
    }

    @Test()
    public void testLessEquals() {
        Constraint expected = new Constraint("first.second", Comparison.LESS_EQUALS, 1l);
        String input = "first.second=le=1l";

        assertValue(expected, input);
    }

    @Test()
    public void testLessThan() {
        Constraint expected = new Constraint("first.second", Comparison.LESS_THAN, 1l);
        String input = "first.second=lt=1L";

        assertValue(expected, input);
    }

    @Test()
    public void testNotEquals() {
        Constraint expected = new Constraint("first.second", Comparison.NOT_EQUALS, 1l);
        String input = "first.second!=1L";

        assertValue(expected, input);
    }

    @Test()
    public void testOneSelector() {
        Constraint expected = new Constraint("first", Comparison.EQUALS, 1);
        String input = "first==1";

        assertValue(expected, input);
    }

    @Test()
    public void testThreeSelector() {
        Constraint expected = new Constraint("first.second.third", Comparison.EQUALS, 1l);
        String input = "first.second.third==1l";

        assertValue(expected, input);
    }

    @Test()
    public void testTwoSelector() {
        Constraint expected = new Constraint("first.second", Comparison.EQUALS, 1);
        String input = "first.second==1";

        assertValue(expected, input);
    }

    @Test()
    public void testUnknownSelector() {
        String input = "unknown==1";

        ParsingResult<Constraint> result = parseRunner.run(input);
        logInfo(result);
        Assert.assertTrue(result.hasErrors());
        Assert.assertFalse(result.matched);
    }

}
