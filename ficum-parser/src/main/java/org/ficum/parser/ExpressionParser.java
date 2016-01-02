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

    @SuppressSubnodes
    protected Rule AndOperation() {
        return Sequence(Ch(Operator.AND.sign), push(Operator.AND));
    }

    protected Rule Expression() {
        return Sequence(FirstOf(SubExpression(), Constraint()),
                ZeroOrMore(FirstOf(AndOperation(), OrOperation()), FirstOf(SubExpression(), Constraint())));
    }

    @SuppressSubnodes
    protected Rule OrOperation() {
        return Sequence(Ch(Operator.OR.sign), push(Operator.OR));
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
        return Sequence(Ch(Operator.LEFT.sign), push(Operator.LEFT), OneOrMore(Expression()), Ch(Operator.RIGHT.sign),
                push(Operator.RIGHT));
    }

}
