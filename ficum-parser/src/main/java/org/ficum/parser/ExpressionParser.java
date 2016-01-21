package org.ficum.parser;

import java.util.ArrayDeque;
import java.util.Deque;

import org.ficum.node.Operator;
import org.parboiled.Action;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressSubnodes;

@BuildParseTree
public class ExpressionParser extends ConstraintParser {

    public ExpressionParser(String... allowedSelectorNames) {
        super(allowedSelectorNames);
    }

    protected Rule Expression() {
        return Sequence(FirstOf(SubExpression(), Constraint()),
                ZeroOrMore(Operation(), FirstOf(SubExpression(), Constraint())));
    }

    @SuppressSubnodes
    protected Rule Operation() {
        return Sequence(FirstOf(Operator.allSigns()), new Action<Object>() {
            public boolean run(Context<Object> context) {
                return push(Operator.from(match()));
            }
        });
    }

    @Override
    public Rule root() {
        return Sequence(Expression(), EOI, new Action<Object>() {
            public boolean run(Context<Object> context) {
                Deque<Object> output = new ArrayDeque<Object>();
                for (Object element : context.getValueStack()) {
                    output.push(element);
                }
                return push(output);
            }
        });
    }

    protected Rule SubExpression() {
        return Sequence(String(Operator.LEFT.getSign()), push(Operator.LEFT), OneOrMore(Expression()),
                String(Operator.RIGHT.getSign()), push(Operator.RIGHT));
    }

}
