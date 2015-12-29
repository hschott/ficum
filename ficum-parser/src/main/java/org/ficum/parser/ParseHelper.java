package org.ficum.parser;

import java.util.Deque;

import org.ficum.node.Builder;
import org.ficum.node.Node;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;

public class ParseHelper {

    public static final Node parse(String query, String... allowedSelectorNames) {
        ExpressionParser parser = Parboiled.createParser(ExpressionParser.class, (Object) allowedSelectorNames);
        BasicParseRunner<Deque<Object>> parseRunner = new BasicParseRunner<Deque<Object>>(parser.root());
        ParsingResult<Deque<Object>> result = parseRunner.run(query);

        if (result.hasErrors()) {
            throw new IllegalArgumentException(ErrorUtils.printParseErrors(result.parseErrors));
        }
        return Builder.build(result.resultValue);
    }

}
