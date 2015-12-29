package org.ficum.parser;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.ficum.node.ISO8601DateFormat;
import org.joda.time.DateTime;
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

    public ArgumentParser() {
        super();
    }

    Rule AnyString(final StringVar literal) {
        return Sequence(NoneOf("'"), new Action<Comparable<?>>() {
            public boolean run(Context<Comparable<?>> context) {
                literal.append(match());
                return true;
            }
        });
    }

    Rule Argument() {
        return FirstOf(TimestampLiteral(), DateLiteral(), FloatLiteral(), DoubleLiteral(), IntegerLiteral(),
                BooleanTrue(), BooleanFalse(), NullLiteral(), StringLiteral());
    }

    @SuppressSubnodes
    Rule BooleanFalse() {
        return Sequence(FirstOf(Sequence(AnyOf("Ff"), String("alse")), Sequence(AnyOf("Nn"), String("o"))),
                push(Boolean.FALSE));
    }

    @SuppressSubnodes
    Rule BooleanTrue() {
        return Sequence(FirstOf(Sequence(AnyOf("Tt"), String("rue")), Sequence(AnyOf("Yy"), String("es"))),
                push(Boolean.TRUE));
    }

    @SuppressSubnodes
    Rule DateLiteral() {
        return Sequence(
                Sequence(Optional(Ch('-')), OneOrMore(Digit()), Ch('-'), Digit(), Digit(), Ch('-'), Digit(), Digit()),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        try {
                            DateTime parse = ISO8601DateFormat.ISO8601_DATE.parseDateTime(match());
                            return push(parse.toDate());
                        } catch (Exception e) {
                            return false;
                        }
                    }
                });
    }

    Rule DecimalNumeral() {
        return FirstOf(Ch('0'), Sequence(CharRange('1', '9'), ZeroOrMore(Digit())));
    }

    Rule Digit() {
        return CharRange('0', '9');
    }

    Rule DoubleDecimal() {
        return FirstOf(
                Sequence(OneOrMore(Digit()), Ch('.'), ZeroOrMore(Digit()), Optional(Exponent()), Optional(AnyOf("dD"))),
                Sequence('.', OneOrMore(Digit()), Optional(Exponent()), Optional(AnyOf("dD"))),
                Sequence(OneOrMore(Digit()), Exponent(), Optional(AnyOf("dD"))),
                Sequence(OneOrMore(Digit()), Optional(Exponent()), AnyOf("dD")));
    }

    @SuppressSubnodes
    Rule DoubleLiteral() {
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

    Rule Exponent() {
        return Sequence(AnyOf("eE"), Optional(AnyOf("-+")), OneOrMore(Digit()));
    }

    Rule FloatDecimal() {
        return FirstOf(
                Sequence(OneOrMore(Digit()), Ch('.'), ZeroOrMore(Digit()), Optional(Exponent()), Optional(AnyOf("fF"))),
                Sequence('.', OneOrMore(Digit()), Optional(Exponent()), Optional(AnyOf("fF"))),
                Sequence(OneOrMore(Digit()), Exponent(), Optional(AnyOf("fF"))),
                Sequence(OneOrMore(Digit()), Optional(Exponent()), AnyOf("fF")));
    }

    @SuppressSubnodes
    Rule FloatLiteral() {
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
    Rule HexDigit() {
        return FirstOf(CharRange('a', 'f'), CharRange('A', 'F'), Digit());
    }

    @SuppressSubnodes
    Rule HexEscape(final StringVar literal) {
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
    Rule IntegerLiteral() {
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

    @SuppressSubnodes
    Rule NullLiteral() {
        return Sequence(Sequence(AnyOf("Nn"), String("ull")), push(null));
    }

    @MemoMismatches
    Rule PctDigit() {
        return FirstOf(CharRange('A', 'F'), Digit());
    }

    @SuppressSubnodes
    Rule PctEncoded(final StringVar literal) {
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
    Rule Sign() {
        return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), Digit(), AnyOf("-+."));
    }

    Rule StringLiteral() {
        StringVar literal = new StringVar();
        return Sequence(Sequence(Ch('\''),
                OneOrMore(FirstOf(PctEncoded(literal), HexEscape(literal), AnyString(literal))), Ch('\'')),
                push(literal.get()));
    }

    @SuppressSubnodes
    Rule TimestampLiteral() {
        return Sequence(
                Sequence(Optional(Ch('-')), OneOrMore(Digit()), Ch('-'), Digit(), Digit(), Ch('-'), Digit(), Digit(),
                        Ch('T'), Digit(), Digit(), Ch(':'), Digit(), Digit(), Ch(':'), Digit(), Digit(),
                        Ch('.'), Digit(), Digit(), Digit(), FirstOf(Ch('Z'), Sequence(AnyOf("-+"), Digit(), Digit(),
                                Ch(':'), Digit(), Digit(), Optional(Ch(':'), Digit(), Digit())))),
                new Action<Comparable<?>>() {
                    public boolean run(Context<Comparable<?>> context) {
                        try {
                            DateTime parse = ISO8601DateFormat.ISO8601_TIMESTAMP.parseDateTime(match());
                            return push(parse.toDate());
                        } catch (Exception e) {
                            return false;
                        }
                    }
                });
    }

}