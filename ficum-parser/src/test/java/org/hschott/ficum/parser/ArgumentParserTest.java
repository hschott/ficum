package org.hschott.ficum.parser;

import org.hschott.ficum.node.AbstractVisitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.common.StringBuilderSink;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.errors.InvalidInputError;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class ArgumentParserTest {
    private static final Logger LOG = LoggerFactory.getLogger(ArgumentParserTest.class);

    private static final ArgumentParser parser = Parboiled.createParser(ArgumentParser.class);

    private TracingParseRunner<Comparable<?>> parseRunner;

    private StringBuilderSink sink;

    private void assertError(Class<?> expected, String input) {
        ParsingResult<Comparable<?>> result = parseRunner.run(input);
        logInfo(result);
        Assert.assertTrue(result.hasErrors());
        Assert.assertEquals(1, result.parseErrors.size());
        Assert.assertTrue(result.parseErrors.getFirst().getClass().isAssignableFrom(expected));
    }

    private void assertValue(Comparable<?> expected, String input) {
        ParsingResult<Comparable<?>> result = parseRunner.run(input);
        logInfo(result);
        Assert.assertFalse(ErrorUtils.printParseErrors(result.parseErrors), result.hasErrors());
        Assert.assertTrue(result.matched);
        Assert.assertEquals(expected, result.resultValue);
    }

    private void logInfo(ParsingResult<Comparable<?>> result) {
        LOG.info(sink.toString());
        if (result.hasErrors()) {
            LOG.info(ErrorUtils.printParseErrors(result.parseErrors));
        } else if (result.matched) {
            LOG.info("NodeTree: {}\n", ParseTreeUtils.printNodeTree(result));
        }
    }

    @Before
    public void setUp() {
        sink = new StringBuilderSink();
        parseRunner = new TracingParseRunner<Comparable<?>>(parser.root()).withLog(sink);
    }

    @Test()
    public void testBooleanfalse() {
        Boolean expected = Boolean.FALSE;
        String input = "false";

        assertValue(expected, input);
    }

    @Test()
    public void testBooleanFalse() {
        Boolean expected = Boolean.FALSE;
        String input = "False";

        assertValue(expected, input);
    }

    @Test()
    public void testBooleanno() {
        Boolean expected = Boolean.FALSE;
        String input = "no";

        assertValue(expected, input);
    }

    @Test()
    public void testBooleanNo() {
        Boolean expected = Boolean.FALSE;
        String input = "No";

        assertValue(expected, input);
    }

    @Test()
    public void testBooleantrue() {
        Boolean expected = Boolean.TRUE;
        String input = "true";

        assertValue(expected, input);
    }

    @Test()
    public void testBooleanTrue() {
        Boolean expected = Boolean.TRUE;
        String input = "True";

        assertValue(expected, input);
    }

    @Test()
    public void testBooleanyes() {
        Boolean expected = Boolean.TRUE;
        String input = "yes";

        assertValue(expected, input);
    }

    @Test()
    public void testBooleanYes() {
        Boolean expected = Boolean.TRUE;
        String input = "Yes";

        assertValue(expected, input);
    }

    @Test()
    public void testDate() {
        LocalDate date = LocalDate.now();
        String input = DateTimeFormatter.ISO_LOCAL_DATE.format(date);

        assertValue(date, input);
    }

    @Test()
    public void testDateAD10000() {
        LocalDate date = LocalDate.now().plusYears(12500);
        String input = DateTimeFormatter.ISO_LOCAL_DATE.format(date);

        assertValue(date, input);
    }

    @Test()
    public void testDateBC() {
        LocalDate date = LocalDate.now().minusYears(12500);
        String input = DateTimeFormatter.ISO_LOCAL_DATE.format(date);

        assertValue(date, input);
    }

    @Test()
    public void testInvalidInput_Decimal() {
        String input = "3213,654";

        assertError(InvalidInputError.class, input);
    }

    @Test()
    public void testInvalidInput_ExponentWithLong() {
        String input = "123E+1L";

        assertError(InvalidInputError.class, input);
    }

    @Test()
    public void testNegative_Decimal() {
        final Double expected = -123.456;
        String input = "-123.456";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_double() {
        final Double expected = -123.456d;
        String input = "-123.456d";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_Double() {
        final Double expected = -123.456d;
        String input = "-123.456D";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_Exponent_Decimal() {
        final Double expected = -123.456;
        String input = "-123456E-3";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_Exponent_double() {
        final Double expected = -123.456d;
        String input = "-123456E-3d";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_Exponent_Double() {
        final Double expected = -123.456d;
        String input = "-123456e-3D";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_Exponent_float() {
        final Float expected = -123.456f;
        String input = "-1.23456e+2f";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_Exponent_Float() {
        final Float expected = -123.456f;
        String input = "-12.3456E+1F";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_float() {
        final Float expected = -123.456f;
        String input = "-123.456f";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_Float() {
        final Float expected = -123.456f;
        String input = "-123.456F";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_Integer() {
        final Integer expected = -123456;
        String input = "-123456";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_long() {
        final Long expected = -123L;
        String input = "-123l";

        assertValue(expected, input);
    }

    @Test()
    public void testNegative_Long() {
        final Long expected = -123L;
        String input = "-123L";

        assertValue(expected, input);
    }

    @Test()
    public void testnull() {
        Comparable<?> expected = null;
        String input = "null";

        assertValue(expected, input);
    }

    @Test()
    public void testNull() {
        Comparable<?> expected = null;
        String input = "Null";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_Decimal() {
        final Double expected = 123.456;
        String input = "+123.456";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_double() {
        final Double expected = 123.456d;
        String input = "+123.456d";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_Double() {
        final Double expected = 123.456d;
        String input = "+123.456D";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_Exponent_Decimal() {
        final Double expected = 123.456;
        String input = "+123456E-3";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_Exponent_double() {
        final Double expected = 123.456d;
        String input = "+123456E-3d";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_Exponent_Double() {
        final Double expected = 123.456d;
        String input = "+123456e-3D";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_Exponent_float() {
        final Float expected = 123.456f;
        String input = "+1.23456e+2f";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_Exponent_Float() {
        final Float expected = 123.456f;
        String input = "+12.3456E+1F";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_float() {
        final Float expected = 123.456f;
        String input = "+123.456f";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_Float() {
        final Float expected = 123.456f;
        String input = "+123.456F";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_Integer() {
        final Integer expected = 123456;
        String input = "+123456";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_long() {
        final Long expected = 123L;
        String input = "+123l";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_Long() {
        final Long expected = 123L;
        String input = "+123L";

        assertValue(expected, input);
    }

    @Test()
    public void testStringEmpty() {
        final String expected = "";
        String input = "''";

        assertValue(expected, input);
    }

    @Test()
    public void testPositive_uuid() {
        final UUID expected = UUID.randomUUID();
        String input = expected.toString();

        assertValue(expected, input);
    }

    @Test()
    public void testError_uppercase_uuid() {
        final UUID expected = UUID.randomUUID();
        String input = expected.toString().toUpperCase();

        assertError(InvalidInputError.class, input);
    }

    @Test()
    public void testError_invalid_uuid() {
        final UUID expected = UUID.randomUUID();
        String input = expected + "a";

        assertError(InvalidInputError.class, input);
    }

    @Test()
    public void testStringHexEncoded() {
        final String expected = "\u3441\u3542\u4343";
        String input = "'0x34410X3542#4343'";

        assertValue(expected, input);
    }

    @Test()
    public void testStringMixedEncodedAny() {
        final String expected = "\u3441$Text\u3542()\u4343";
        String input = "'0x3441%24Text0X3542%28%29#4343'";

        assertValue(expected, input);
    }

    @Test()
    public void testStringOneChar() {
        final Character expected = '\u0522';
        String input = "'%D4%A2'";

        assertValue(expected, input);
    }

    @Test()
    public void testStringPctEncoded() {
        final String expected = "/\u00a7$()";
        String input = "'%2F%C2%A7%24%28%29'";

        assertValue(expected, input);
    }

    @Test()
    public void testTimestamp() {
        OffsetDateTime dateTime = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.ofHours(3)).truncatedTo(ChronoUnit.MILLIS);
        String input = AbstractVisitor.ISO_OFFSET_DATE_TIME.format(dateTime);

        assertValue(dateTime, input);
    }

    @Test()
    public void testTimestampAD10000() {
        OffsetDateTime dateTime = OffsetDateTime.now().plusYears(10000).truncatedTo(ChronoUnit.MILLIS);
        String input = AbstractVisitor.ISO_OFFSET_DATE_TIME.format(dateTime);

        assertValue(dateTime, input);
    }

    @Test()
    public void testTimestampBC() {
        OffsetDateTime dateTime = OffsetDateTime.now().minusYears(2500).truncatedTo(ChronoUnit.MILLIS);
        String input = AbstractVisitor.ISO_OFFSET_DATE_TIME.format(dateTime);

        assertValue(dateTime, input);
    }

    @Test()
    public void testTimestampZulu() {
        OffsetDateTime dateTime = OffsetDateTime.now(ZoneId.of("Z")).truncatedTo(ChronoUnit.MILLIS);
        String input = AbstractVisitor.ISO_OFFSET_DATE_TIME.format(dateTime);

        assertValue(dateTime, input);
    }

    @Test()
    public void testUnsigned_Decimal() {
        final Double expected = 123.456;
        String input = "123.456";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_double() {
        final Double expected = 123.456d;
        String input = "123.456d";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_Double() {
        final Double expected = 123.456d;
        String input = "123.456D";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_Exponent_Decimal() {
        final Double expected = 123.456;
        String input = "123456E-3";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_Exponent_double() {
        final Double expected = 123.456d;
        String input = "123456E-3d";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_Exponent_Double() {
        final Double expected = 123.456d;
        String input = "123456e-3D";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_Exponent_float() {
        final Float expected = 123.456f;
        String input = "12345.6e-2f";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_Exponent_Float() {
        final Float expected = 123.456f;
        String input = "12.3456E+1F";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_float() {
        final Float expected = 123.456f;
        String input = "123.456f";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_Float() {
        final Float expected = 123.456f;
        String input = "123.456F";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_Integer() {
        final Integer expected = 123456;
        String input = "123456";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_long() {
        final Long expected = 123L;
        String input = "123l";

        assertValue(expected, input);
    }

    @Test()
    public void testUnsigned_Long() {
        final Long expected = 123L;
        String input = "123L";

        assertValue(expected, input);
    }

}
