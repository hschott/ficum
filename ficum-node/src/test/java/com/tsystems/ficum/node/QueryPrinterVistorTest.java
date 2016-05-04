package com.tsystems.ficum.node;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.chrono.GregorianChronology;
import org.junit.Assert;
import org.junit.Test;

public class QueryPrinterVistorTest {

    @Test
    public void testBuilderBoolean() {
        String expected = "first==true,second==false";

        Node node = Builder.start().constraint("first", Comparison.EQUALS, true).and()
                .constraint("second", Comparison.EQUALS, false).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderByte() {
        String expected = "first=gt=127";

        byte byteVar = 127;

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, byteVar).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderCharacter() {
        String expected = "first=gt='H'";

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, 'H').build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderDateTime() {
        String expected = "first==-0701-01-02T03:55:56.234-03:00";

        Node node = Builder.start()
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
        String expected = "first=gt=232.34";

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, 232.34).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderEnum() {
        String expected = "first=='ET'";

        Node node = Builder.start().constraint("first", Comparison.EQUALS, Aliens.ET).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderFloat() {
        String expected = "first=gt=23.234f";

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, 23.234f).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderInteger() {
        String expected = "first=gt=23234";

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, 23234).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderIterable() {
        String expected = "first=wi=[23,3.5f,7.02,null,true,'hello hello']";

        Node node = Builder.start()
                .constraint("first", Comparison.WITHIN, 23, 3.5f, 7.02, (Comparable<?>) null, true, "hello hello")
                .build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderLocalDate() {
        String expected = "first==1986-01-23";

        Node node = Builder.start().constraint("first", Comparison.EQUALS,
                new LocalDate().withYear(1986).withMonthOfYear(1).withDayOfMonth(23)).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderLong() {
        String expected = "first=gt=45l";

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, 45l).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderNull() {
        String expected = "first!=null";
        Node node = Builder.start().constraint("first", Comparison.NOT_EQUALS, (Comparable<?>) null).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testBuilderShort() {
        String expected = "first=gt=32767";

        short shortVar = 32767;

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, shortVar).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDate() {
        Node node = Builder.start().constraint("first", Comparison.EQUALS,
                new LocalDate().withYear(2015).withMonthOfYear(12).withDayOfMonth(29)).build();

        String expected = "first==2015-12-29";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDateBC() {
        Node node = Builder.start().constraint("first", Comparison.EQUALS,
                new LocalDate().withYear(-650).withMonthOfYear(12).withDayOfMonth(29)).build();

        String expected = "first==-0650-12-29";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNaturalOrder() {
        Node node = Builder.start().constraint("first", Comparison.EQUALS, 1l).and()
                .constraint("second", Comparison.GREATER_THAN, "two").or()
                .constraint("third", Comparison.LESS_EQUALS, 3.0f).build();

        String expected = "first==1l,second=gt='two';third=le=3.0f";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNestedPreceded() {
        Node node = Builder.start().constraint("first", Comparison.EQUALS, 1l).and().sub().sub()
                .constraint("second", Comparison.GREATER_THAN, "two").or()
                .constraint("third", Comparison.LESS_EQUALS, 3.0f).endsub().or()
                .constraint("fourth", Comparison.LESS_THAN, "five").endsub().build();

        String expected = "first==1l,((second=gt='two';third=le=3.0f);fourth=lt='five')";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPrecededOrder() {

        Node node = Builder.start().sub().constraint("first", Comparison.EQUALS, 1l).or()
                .constraint("second", Comparison.GREATER_THAN, "two").endsub().and().sub()
                .constraint("third", Comparison.LESS_EQUALS, 3.0f).or()
                .constraint("fourth", Comparison.LESS_THAN, "five").endsub().build();

        String expected = "(first==1l;second=gt='two'),(third=le=3.0f;fourth=lt='five')";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTimestampBC() {
        Node node = Builder.start()
                .constraint("first", Comparison.EQUALS,
                        new DateTime(GregorianChronology.getInstance(DateTimeZone.forOffsetHours(0))).withYear(-456)
                                .withMonthOfYear(3).withDayOfMonth(19).withHourOfDay(18).withMinuteOfHour(34)
                                .withSecondOfMinute(12).withMillisOfSecond(0))
                .build();

        String expected = "first==-0456-03-19T18:34:12.000Z";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTimestampZoneOffset() {
        Node node = Builder.start()
                .constraint("first", Comparison.EQUALS,
                        new DateTime(DateTimeZone.forOffsetHours(5)).withYear(2015).withMonthOfYear(12)
                                .withDayOfMonth(29).withHourOfDay(18).withMinuteOfHour(34).withSecondOfMinute(12)
                                .withMillisOfSecond(0))
                .build();

        String expected = "first==2015-12-29T18:34:12.000+05:00";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTimestampZulu() {
        Node node = Builder.start()
                .constraint("first", Comparison.EQUALS,
                        new DateTime(DateTimeZone.forOffsetHours(0)).withYear(2015).withMonthOfYear(12)
                                .withDayOfMonth(29).withHourOfDay(18).withMinuteOfHour(34).withSecondOfMinute(12)
                                .withMillisOfSecond(0))
                .build();

        String expected = "first==2015-12-29T18:34:12.000Z";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    private enum Aliens {
        ET
    }

}
