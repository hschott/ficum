package com.tsystems.ficum.parser;

import java.util.Deque;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;

import com.tsystems.ficum.node.Builder;
import com.tsystems.ficum.node.Comparison;
import com.tsystems.ficum.node.Node;
import com.tsystems.ficum.parser.ConstraintParser;
import com.tsystems.ficum.parser.ExpressionParser;

public class ExpressionParserToBuilderEqualityTest {

    private BasicParseRunner<Deque<Object>> parseRunner;

    private void assertSameNodetree(String input, Node expected) {
        ParsingResult<Deque<Object>> result = parseRunner.run(input);
        Node actual = Builder.build(result.resultValue);

        Assert.assertEquals(expected, actual);
    }

    @Before
    public void setUp() {
        String[] allowedPaths = { "first", "second", "third", "fourth", "fifth" };
        ConstraintParser parser = Parboiled.createParser(ExpressionParser.class, (Object) allowedPaths);

        parseRunner = new BasicParseRunner<Deque<Object>>(parser.root());
    }

    @Test
    public void testAndOperator() {
        String input = "first==1l,second=gt='two'";

        Node expected = Builder.newInstance().constraint("first", Comparison.EQUALS, 1l).and()
                .constraint("second", Comparison.GREATER_THAN, "two").build();

        assertSameNodetree(input, expected);
    }

    @Test
    public void testNaturalOrderLeft() {
        String input = "first==1l,second=gt='two';third=le=3f";

        Node expected = Builder.newInstance().constraint("first", Comparison.EQUALS, 1l).and()
                .constraint("second", Comparison.GREATER_THAN, "two").or()
                .constraint("third", Comparison.LESS_EQUALS, 3f).build();

        assertSameNodetree(input, expected);
    }

    @Test
    public void testNaturalOrderRight() {
        String input = "first==1l;second=gt='two',third=le=3f";

        Node expected = Builder.newInstance().constraint("first", Comparison.EQUALS, 1l).or()
                .constraint("second", Comparison.GREATER_THAN, "two").and()
                .constraint("third", Comparison.LESS_EQUALS, 3f).build();

        assertSameNodetree(input, expected);
    }

    @Test
    public void testNestedPreceded() {
        String input = "first=gt=1,(second=le=2l;(third=gt=3;fourth==4f),fifth=lt='five')";

        Node expected = Builder.newInstance().constraint("first", Comparison.GREATER_THAN, 1).and().sub()
                .constraint("second", Comparison.LESS_EQUALS, 2l).or().sub()
                .constraint("third", Comparison.GREATER_THAN, 3).or().constraint("fourth", Comparison.EQUALS, 4f)
                .endsub().and().constraint("fifth", Comparison.LESS_THAN, "five").endsub().build();

        assertSameNodetree(input, expected);
    }

    @Test
    public void testOrOperator() {
        String input = "first==1l;second=gt='two'";

        Node expected = Builder.newInstance().constraint("first", Comparison.EQUALS, 1l).or()
                .constraint("second", Comparison.GREATER_THAN, "two").build();

        assertSameNodetree(input, expected);
    }

    @Test
    public void testPrecededOrderLeft() {
        String input = "(first==1l;second=gt='two'),third=le=3f";

        Node expected = Builder.newInstance().sub().constraint("first", Comparison.EQUALS, 1l).or()
                .constraint("second", Comparison.GREATER_THAN, "two").endsub().and()
                .constraint("third", Comparison.LESS_EQUALS, 3f).build();

        assertSameNodetree(input, expected);
    }

    @Test
    public void testPrecededOrderRight() {
        String input = "first==1l,(second=gt='two';third=le=3f)";

        Node expected = Builder.newInstance().constraint("first", Comparison.EQUALS, 1l).and().sub()
                .constraint("second", Comparison.GREATER_THAN, "two").or()
                .constraint("third", Comparison.LESS_EQUALS, 3f).endsub().build();

        assertSameNodetree(input, expected);
    }

    @Test
    public void testSimpleConstraint() {
        String input = "first==1l";

        Node expected = Builder.newInstance().constraint("first", Comparison.EQUALS, 1l).build();

        assertSameNodetree(input, expected);
    }
}
