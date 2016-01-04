package org.ficum.visitor;

import org.apache.commons.lang3.JavaVersion;
import org.ficum.node.Builder;
import org.ficum.node.Comparison;
import org.ficum.node.Node;
import org.ficum.parser.ParseHelper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.junit.Assert;
import org.junit.Test;

public class QueryPrinterVistorTest {
    private String[] allowedSelectorNames = { "first", "second", "third", "fourth", "fifth" };

    @Test
    public void testBuilderDateTime() {
        String expected = "first==-0701-01-02T03:55:56.234-03:00";

        Node node = Builder.newInstance()
                .constraint("first", Comparison.EQUALS,
                        new DateTime(DateTimeZone.forOffsetHours(-3)).withYear(-701).withMonthOfYear(1)
                                .withDayOfMonth(2).withHourOfDay(3).withMinuteOfHour(55).withSecondOfMinute(56)
                                .withMillisOfSecond(234))
                .build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderDouble() {
        String expected = "first=gt=232.34d";

        Node node = Builder.newInstance().constraint("first", Comparison.GREATER_THAN, 232.34).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderEnum() {
        String expected = "first=='JAVA_1_6'";

        Node node = Builder.newInstance().constraint("first", Comparison.EQUALS, JavaVersion.JAVA_1_6).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderFloat() {
        String expected = "first=gt=23.234";

        Node node = Builder.newInstance().constraint("first", Comparison.GREATER_THAN, 23.234f).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderInteger() {
        String expected = "first=gt=23234";

        Node node = Builder.newInstance().constraint("first", Comparison.GREATER_THAN, 23234).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderLocalDate() {
        String expected = "first==1986-01-23";

        Node node = Builder.newInstance().constraint("first", Comparison.EQUALS,
                new LocalDate().withYear(1986).withMonthOfYear(1).withDayOfMonth(23)).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderLong() {
        String expected = "first=gt=45l";

        Node node = Builder.newInstance().constraint("first", Comparison.GREATER_THAN, 45l).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testBuilderYearMonthDay() {
        String expected = "first==1986-01-23";

        Node node = Builder.newInstance().constraint("first", Comparison.EQUALS,
                new YearMonthDay().withYear(1986).withMonthOfYear(1).withDayOfMonth(23)).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

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
