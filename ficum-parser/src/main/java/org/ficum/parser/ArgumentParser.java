package org.ficum.parser;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.chrono.GJChronology;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.MemoMismatches;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.StringVar;

@BuildParseTree
public class ArgumentParser extends BaseParser<Object> {

    private static final Set<Class<?>> baseTypes = new HashSet<Class<?>>();

    static {
        baseTypes.add(Character.class);
        baseTypes.add(String.class);
        baseTypes.add(Float.class);
        baseTypes.add(Double.class);
        baseTypes.add(Integer.class);
        baseTypes.add(Long.class);
        baseTypes.add(Boolean.class);
        baseTypes.add(Calendar.class);
    }

    protected static final DateTimeFormatter ISO8601_TIMESTAMP = ISODateTimeFormat.dateTime().withOffsetParsed()
            .withChronology(GJChronology.getInstance());

    protected static final DateTimeFormatter ISO8601_DATE = ISODateTimeFormat.yearMonthDay()
            .withChronology(GJChronology.getInstance(DateTimeZone.UTC));

    public static Collection<Class<?>> getBaseTypes() {
        return Collections.unmodifiableCollection(baseTypes);
    }

    protected Rule AnyString(final StringVar literal) {
        return Sequence(NoneOf("'"), new Action<Comparable<?>>() {
            public boolean run(Context<Comparable<?>> context) {
                literal.append(match());
                return true;
            }
        });
    }

    protected Rule Argument() {
        return Sequence(FirstOf(TimestampLiteral(), DateLiteral(), DoubleLiteral(), FloatLiteral(), IntegerLiteral(),
                BooleanTrue(), BooleanFalse(), NullLiteral(), StringLiteral()), new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        Comparable<?> argument = context.getValueStack().peek();
                        return isBaseType(argument);
                    }

                });
    }

    @SuppressSubnodes
    protected Rule BooleanFalse() {
        return Sequence(FirstOf(Sequence(AnyOf("Ff"), String("alse")), Sequence(AnyOf("Nn"), String("o"))),
                push(Boolean.FALSE));
    }

    @SuppressSubnodes
    protected Rule BooleanTrue() {
        return Sequence(FirstOf(Sequence(AnyOf("Tt"), String("rue")), Sequence(AnyOf("Yy"), String("es"))),
                push(Boolean.TRUE));
    }

    @SuppressSubnodes
    protected Rule DateLiteral() {
        return Sequence(
                Sequence(Optional(Ch('-')), OneOrMore(Digit()), Ch('-'), Digit(), Digit(), Ch('-'), Digit(), Digit()),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        try {
                            LocalDate parse = ISO8601_DATE.parseLocalDate(match());
                            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            cal.clear();
                            cal.setTimeInMillis(parse.toDateTimeAtStartOfDay(DateTimeZone.UTC).getMillis());
                            return push(cal);
                        } catch (Exception e) {
                            return false;
                        }
                    }
                });
    }

    protected Rule DecimalNumeral() {
        return FirstOf(Ch('0'), Sequence(CharRange('1', '9'), ZeroOrMore(Digit())));
    }

    protected Rule Digit() {
        return CharRange('0', '9');
    }

    protected Rule DoubleDecimal() {
        return FirstOf(
                Sequence(OneOrMore(Digit()), Ch('.'), ZeroOrMore(Digit()), Optional(Exponent()), Optional(AnyOf("dD"))),
                Sequence('.', OneOrMore(Digit()), Optional(Exponent()), Optional(AnyOf("dD"))),
                Sequence(OneOrMore(Digit()), Exponent(), Optional(AnyOf("dD"))),
                Sequence(OneOrMore(Digit()), Optional(Exponent()), AnyOf("dD")));
    }

    @SuppressSubnodes
    protected Rule DoubleLiteral() {
        return Sequence(Sequence(Optional(AnyOf("-+")), DoubleDecimal(), TestNot(Sign())), new Action<Comparable<?>>() {
            public boolean run(Context<Comparable<?>> context) {
                try {
                    Double valueOf = Double.parseDouble(match());
                    return push(valueOf);
                } catch (NumberFormatException e) {
                    return false;
                }
            }

        });
    }

    protected Rule Exponent() {
        return Sequence(AnyOf("eE"), Optional(AnyOf("-+")), OneOrMore(Digit()));
    }

    protected Rule FloatDecimal() {
        return FirstOf(
                Sequence(OneOrMore(Digit()), Ch('.'), ZeroOrMore(Digit()), Optional(Exponent()), Optional(AnyOf("fF"))),
                Sequence('.', OneOrMore(Digit()), Optional(Exponent()), Optional(AnyOf("fF"))),
                Sequence(OneOrMore(Digit()), Exponent(), Optional(AnyOf("fF"))),
                Sequence(OneOrMore(Digit()), Optional(Exponent()), AnyOf("fF")));
    }

    @SuppressSubnodes
    protected Rule FloatLiteral() {
        return Sequence(Sequence(Optional(AnyOf("-+")), FloatDecimal(), TestNot(Sign())), new Action<Comparable<?>>() {
            public boolean run(Context<Comparable<?>> context) {
                try {
                    Float valueOf = Float.parseFloat(match());
                    return push(valueOf);
                } catch (NumberFormatException e) {
                    return false;
                }
            }

        });
    }

    @MemoMismatches
    protected Rule HexDigit() {
        return FirstOf(CharRange('a', 'f'), CharRange('A', 'F'), Digit());
    }

    @SuppressSubnodes
    protected Rule HexEscape(final StringVar literal) {
        return Sequence(
                Sequence(FirstOf(Ch('#'), Sequence(Ch('0'), AnyOf("xX"))), OneOrMore(Sequence(HexDigit(), HexDigit()))),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        literal.append(new String(Character.toChars(Integer.decode(match()).intValue())));
                        return true;
                    }
                });
    }

    @SuppressSubnodes
    protected Rule IntegerLiteral() {
        return Sequence(Sequence(Optional(AnyOf("-+")), DecimalNumeral(), Optional(AnyOf("lL")), TestNot(Sign())),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        try {
                            boolean isLong = false;
                            String match = match();
                            int index = match.indexOf("l");
                            if (index == -1) {
                                index = match.indexOf("L");
                            }
                            if (index != -1) {
                                isLong = true;
                                match = match.substring(0, index);
                            }
                            index = match.indexOf("+");
                            if (index == 0) {
                                match = match.substring(1, match.length());
                            }
                            if (isLong) {
                                Long valueOf = Long.parseLong(match);
                                return push(valueOf);
                            } else {
                                Integer valueOf = Integer.parseInt(match);
                                return push(valueOf);
                            }
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                });
    }

    protected boolean isBaseType(Comparable<?> type) {
        if (type == null) {
            return true;
        }

        Class<?> clazz = type.getClass();
        for (Class<?> baseType : baseTypes) {
            if (baseType.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    @SuppressSubnodes
    protected Rule NullLiteral() {
        return Sequence(Sequence(AnyOf("Nn"), String("ull")), push(null));
    }

    @MemoMismatches
    protected Rule PctDigit() {
        return FirstOf(CharRange('A', 'F'), Digit());
    }

    @SuppressSubnodes
    protected Rule PctEncoded(final StringVar literal) {
        return Sequence(OneOrMore(Sequence(Ch('%'), PctDigit(), PctDigit())), new Action<Comparable<?>>() {
            public boolean run(Context<Comparable<?>> context) {
                try {
                    literal.append(URLDecoder.decode(match(), "utf-8"));
                    return true;
                } catch (UnsupportedEncodingException e) {
                    return false;
                }
            }
        });
    }

    public Rule root() {
        return Sequence(Argument(), EOI);
    }

    @MemoMismatches
    protected Rule Sign() {
        return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), Digit(), AnyOf("-+."));
    }

    protected Rule StringLiteral() {
        final StringVar literal = new StringVar();
        return Sequence(Sequence(Ch('\''),
                ZeroOrMore(FirstOf(PctEncoded(literal), HexEscape(literal), AnyString(literal))), Ch('\'')),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        String value = literal.get();
                        if (value == null) {
                            return push("");
                        }
                        if (value.length() == 1) {
                            return push(new Character(value.charAt(0)));
                        }
                        return push(value);

                    }
                });
    }

    @SuppressSubnodes
    protected Rule TimestampLiteral() {
        return Sequence(
                Sequence(Optional(Ch('-')), OneOrMore(Digit()), Ch('-'), Digit(), Digit(), Ch('-'), Digit(), Digit(),
                        Ch('T'), Digit(), Digit(), Ch(':'), Digit(), Digit(), Ch(':'), Digit(), Digit(),
                        Ch('.'), Digit(), Digit(), Digit(), FirstOf(Ch('Z'), Sequence(AnyOf("-+"), Digit(), Digit(),
                                Ch(':'), Digit(), Digit(), Optional(Ch(':'), Digit(), Digit())))),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        try {
                            DateTime parse = ISO8601_TIMESTAMP.parseDateTime(match());
                            Calendar cal = Calendar.getInstance(parse.getZone().toTimeZone());
                            cal.clear();
                            cal.setTimeInMillis(parse.getMillis());
                            return push(cal);
                        } catch (Exception e) {
                            return false;
                        }
                    }
                });
    }

}