package org.ficum.visitor;

import org.ficum.node.Node;
import org.ficum.parser.ParseHelper;
import org.junit.Assert;
import org.junit.Test;

public class QueryPrinterVistorTest {
    private String[] allowedSelectorNames = { "first", "second", "third", "fourth", "fifth" };

    @Test
    public void testDate() {
        String input = "first==2015-12-29";
        Node node = ParseHelper.parse(input, allowedSelectorNames);
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(input, actual);
    }

    @Test
    public void testDateBC() {
        String input = "first==-0650-12-29";
        Node node = ParseHelper.parse(input, allowedSelectorNames);
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(input, actual);
    }

    @Test
    public void testNaturalOrder() {
        String input = "first==1l,second=gt='two';third=le=3.0";
        Node node = ParseHelper.parse(input, allowedSelectorNames);
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(input, actual);
    }

    @Test
    public void testNestedPreceded() {
        String input = "first==1l,((second=gt='two';third=le=3.0);fourth=lt='five')";
        Node node = ParseHelper.parse(input, allowedSelectorNames);
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(input, actual);
    }

    @Test
    public void testPrecededOrder() {
        String input = "(first==1l;second=gt='two'),(third=le=3.0;fourth=lt='five')";
        Node node = ParseHelper.parse(input, allowedSelectorNames);
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(input, actual);
    }

    @Test
    public void testTimestampBC() {
        String input = "first==-0456-03-19T18:34:12.000Z";
        Node node = ParseHelper.parse(input, allowedSelectorNames);
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(input, actual);
    }

    @Test
    public void testTimestampZoneOffset() {
        String input = "first==2015-12-29T18:34:12.000+05:00";
        Node node = ParseHelper.parse(input, allowedSelectorNames);
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(input, actual);
    }

    @Test
    public void testTimestampZulu() {
        String input = "first==2015-12-29T18:34:12.000Z";
        Node node = ParseHelper.parse(input, allowedSelectorNames);
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(input, actual);
    }

}
