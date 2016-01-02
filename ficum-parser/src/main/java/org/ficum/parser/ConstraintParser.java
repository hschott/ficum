package org.ficum.parser;

import java.util.Arrays;
import java.util.Comparator;

import org.ficum.node.Comparison;
import org.ficum.node.Constraint;
import org.parboiled.Action;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressSubnodes;

@BuildParseTree
public class ConstraintParser extends ArgumentParser {

    protected String[] allowedSelectorNames = {};

    public ConstraintParser(String... allowedSelectorNames) {
        super();
        this.allowedSelectorNames = allowedSelectorNames;
        Arrays.sort(this.allowedSelectorNames, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o2.compareTo(o1);
            }
        });
    }

    protected Rule Comparison() {
        return FirstOf(Equals(), NotEquals(), LowerEquals(), GreaterEquals(), LowerThen(), GreaterThen());
    }

    protected Rule Constraint() {
        return Sequence(Selector(), Comparison(), Argument(), new Action<Object>() {
            public boolean run(Context<Object> context) {
                Comparable<?> argument = (Comparable<?>) pop();
                Comparison comparison = (Comparison) pop();
                String selector = (String) pop();
                return push(new Constraint(selector, comparison, argument));
            }
        });
    }

    @SuppressSubnodes
    protected Rule Equals() {
        return Sequence(String(Comparison.EQUALS.sign), push(Comparison.EQUALS));
    }

    @SuppressSubnodes
    protected Rule GreaterEquals() {
        return Sequence(String(Comparison.GREATER_EQUALS.sign), push(Comparison.GREATER_EQUALS));
    }

    @SuppressSubnodes
    protected Rule GreaterThen() {
        return Sequence(String(Comparison.GREATER_THAN.sign), push(Comparison.GREATER_THAN));
    }

    @SuppressSubnodes
    protected Rule LowerEquals() {
        return Sequence(String(Comparison.LESS_EQUALS.sign), push(Comparison.LESS_EQUALS));
    }

    @SuppressSubnodes
    protected Rule LowerThen() {
        return Sequence(String(Comparison.LESS_THAN.sign), push(Comparison.LESS_THAN));
    }

    @SuppressSubnodes
    protected Rule NotEquals() {
        return Sequence(String(Comparison.NOT_EQUALS.sign), push(Comparison.NOT_EQUALS));
    }

    @Override
    public Rule root() {
        return Sequence(Constraint(), EOI);
    }

    @SuppressSubnodes
    protected Rule Selector() {
        return Sequence(Sequence(FirstOf(allowedSelectorNames), ZeroOrMore(Ch('.'), FirstOf(allowedSelectorNames))),
                push(match()));
    }

}