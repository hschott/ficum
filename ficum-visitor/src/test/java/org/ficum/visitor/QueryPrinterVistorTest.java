package org.ficum.visitor;

import java.util.Deque;

import org.ficum.node.Builder;
import org.ficum.node.Node;
import org.ficum.parser.ConstraintParser;
import org.ficum.parser.ExpressionParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;

public class QueryPrinterVistorTest {
    private BasicParseRunner<Deque<Object>> parseRunner;

    @Before
    public void setUp() {
        String[] allowedPaths = { "first", "second", "third", "fourth", "fifth" };
        ConstraintParser parser = Parboiled.createParser(ExpressionParser.class, (Object) allowedPaths);

        parseRunner = new BasicParseRunner<Deque<Object>>(parser.root());
    }

    @Test
    public void testNaturalOrder() {
        String input = "first==1l,second=gt='two';third=le=3.0";
        ParsingResult<Deque<Object>> result = parseRunner.run(input);
        Node node = Builder.build(result.resultValue);

        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(input, actual);
    }

    @Test
    public void testNestedPreceded() {
        String input = "first==1l,((second=gt='two';third=le=3.0);fourth=lt='five')";
        ParsingResult<Deque<Object>> result = parseRunner.run(input);
        Node node = Builder.build(result.resultValue);

        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(input, actual);
    }

    @Test
    public void testPrecededOrder() {
        String input = "(first==1l;second=gt='two'),(third=le=3.0;fourth=lt='five')";
        ParsingResult<Deque<Object>> result = parseRunner.run(input);
        Node node = Builder.build(result.resultValue);

        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(input, actual);
    }

}
