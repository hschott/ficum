package org.hschott.ficum.node;

import org.junit.Assert;
import org.junit.Test;

import java.time.*;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

public class QueryPrinterVistorTest {

    @Test
    public void testBoolean() {
        String expected = "first==true,second==false";

        Node node = Builder.start().constraint("first", Comparison.EQUALS, true).and()
                           .constraint("second", Comparison.EQUALS, false).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testByte() {
        String expected = "first=gt=127";

        byte byteVar = 127;

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, byteVar).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testCharacter() {
        String expected = "first=gt='H'";

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, 'H').build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDateTime() {
        String expected = "first==-0701-01-02T03:55:56.234-03:00";

        Node node = Builder.start()
                           .constraint("first", Comparison.EQUALS,
                                       OffsetDateTime.of(-701, 1, 2,
                                                         3, 55, 56, 234000000,
                                                         ZoneOffset.ofHours(-3))).build();

        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDouble() {
        String expected = "first=gt=2.3234E12";

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, 2.3234e+12).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testEnum() {
        String expected = "first=='ET'";

        Node node = Builder.start().constraint("first", Comparison.EQUALS, Aliens.ET).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testUuid() {
        UUID uuid = UUID.randomUUID();
        String expected = "first==" + uuid;

        Node node = Builder.start().constraint("first", Comparison.EQUALS, uuid).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFloat() {
        String expected = "first=gt=23.234f";

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, 23.234f).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testInteger() {
        String expected = "first=gt=23234";

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, 23234).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testWithinIterable() {
        String expected = "first=wi=[23,3.5f,7.02,null,true,'hello hello']";

        Node node = Builder.start()
                           .constraint("first", Comparison.WITHIN, 23, 3.5f, 7.02, null, true, "hello hello")
                           .build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testInIterable() {
        String expected = "first=in=[-23,3.5f,7.02,null,true,'hello hello']";

        Node node = Builder.start()
                           .constraint("first", Comparison.IN, -23, 3.5f, 7.02, null, true, "hello hello")
                           .build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNinIterable() {
        String expected = "first=nin=[23,3.5f,7.02,null,true,'hello hello']";

        Node node = Builder.start()
                           .constraint("first", Comparison.NIN, 23, 3.5f, 7.02, null, true, "hello hello")
                           .build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testLong() {
        String expected = "first=gt=45L";

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, 45L).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNullArgument() {
        String actual = new QueryPrinterVisitor().start(null);
        Assert.assertEquals(null, actual);
    }

    @Test
    public void testNull() {
        String expected = "first!=null";
        Node node = Builder.start().constraint("first", Comparison.NOT_EQUALS, (Comparable<?>) null).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testShort() {
        String expected = "first=gt=32767";

        short shortVar = 32767;

        Node node = Builder.start().constraint("first", Comparison.GREATER_THAN, shortVar).build();
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testLocalDate() {
        Node node = Builder.start().constraint("first", Comparison.EQUALS,
                                               LocalDate.of(2015, 12, 29)).build();

        String expected = "first==2015-12-29";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testLocalDateBC() {
        Node node = Builder.start().constraint("first", Comparison.EQUALS,
                                               LocalDate.of(-650, 12, 29)).build();

        String expected = "first==-0650-12-29";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNaturalOrder() {
        Node node = Builder.start().constraint("first", Comparison.EQUALS, 1L).and()
                           .constraint("second", Comparison.GREATER_THAN, "two").or()
                           .constraint("third", Comparison.LESS_EQUALS, 3.0f).build();

        String expected = "first==1L,second=gt='two';third=le=3.0f";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNestedPreceded() {
        Node node = Builder.start().constraint("first", Comparison.EQUALS, 1L).and().sub().sub()
                           .constraint("second", Comparison.GREATER_THAN, "two").or()
                           .constraint("third", Comparison.LESS_EQUALS, 3.0f).endSub().or()
                           .constraint("fourth", Comparison.LESS_THAN, "five").endSub().build();

        String expected = "first==1L,((second=gt='two';third=le=3.0f);fourth=lt='five')";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testPrecededOrder() {

        Node node = Builder.start().sub().constraint("first", Comparison.EQUALS, 1L).or()
                           .constraint("second", Comparison.GREATER_THAN, "two").endSub().and().sub()
                           .constraint("third", Comparison.LESS_EQUALS, 3.0f).or()
                           .constraint("fourth", Comparison.LESS_THAN, "five").endSub().build();

        String expected = "(first==1L;second=gt='two'),(third=le=3.0f;fourth=lt='five')";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTimestampBC() {
        Node node = Builder.start()
                           .constraint("first", Comparison.EQUALS,
                                       OffsetDateTime.of(-456, 3, 19,
                                                         18, 34, 12, 0,
                                                         ZoneOffset.ofHours(0)))
                           .build();

        String expected = "first==-0456-03-19T18:34:12.000Z";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTimestampZoneOffset() {
        Node node = Builder.start()
                           .constraint("first", Comparison.EQUALS,
                                       OffsetDateTime.of(2015, 12, 29,
                                                         18, 34, 12, 0,
                                                         ZoneOffset.ofHours(5)))
                           .build();

        String expected = "first==2015-12-29T18:34:12.000+05:00";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testTimestampZulu() {
        Node node = Builder.start()
                           .constraint("first", Comparison.EQUALS,
                                       OffsetDateTime.of(2015, 12, 29,
                                                         18, 34, 12, 0,
                                                         ZoneOffset.ofHours(0)))
                           .build();

        String expected = "first==2015-12-29T18:34:12.000Z";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDate() {
        LocalDateTime dateTime = LocalDateTime.of(2015, 12, 29,
                                                  18, 34, 12, 0);
        Node node = Builder.start()
                           .constraint("first", Comparison.EQUALS,
                                       Date.from(dateTime.toInstant(
                                               ZoneOffset.systemDefault().getRules().getOffset(dateTime))))
                           .build();

        String expected = "first==2015-12-29T18:34:12.000" + ZoneOffset.systemDefault().getRules().getOffset(dateTime);
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testLocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2015, 12, 29,
                                                  18, 34, 12, 0);
        Node node = Builder.start()
                           .constraint("first", Comparison.EQUALS,
                                       dateTime)
                           .build();

        String expected = "first==2015-12-29T18:34:12.000" + ZoneOffset.systemDefault().getRules().getOffset(dateTime);
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testCalendar() {
        Node node = Builder.start()
                           .constraint("first", Comparison.EQUALS,
                                       GregorianCalendar.from(ZonedDateTime.of(2015, 12, 29,
                                                                               18, 34, 12, 0, ZoneId.of("Z"))))
                           .build();

        String expected = "first==2015-12-29T18:34:12.000Z";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTime() {
        Node node = Builder.start()
                           .constraint("first", Comparison.EQUALS,
                                       ZonedDateTime.of(2015, 12, 29,
                                                        18, 34, 12, 0, ZoneId.of("Z")))
                           .build();

        String expected = "first==2015-12-29T18:34:12.000Z";
        String actual = new QueryPrinterVisitor().start(node);

        Assert.assertEquals(expected, actual);
    }

    private enum Aliens {
        ET
    }

}
