package nl.ramsolutions.sw.magik.api;

import com.sonar.sslr.api.Rule;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.internal.matchers.ParseNode;
import org.sonar.sslr.parser.ParsingResult;

/**
 * Forbid a rule to have matched.
 */
public class MagikRuleForbiddenAssert extends MagikRuleAssert {

    /**
     * Forbidden rule used assertion error.
     */
    static class ForbiddenRuleUsed extends AssertionError {

        private static final long serialVersionUID = 1L;

        ForbiddenRuleUsed(GrammarRuleKey grammarRuleKey) {
            super("Forbidden use of: " + grammarRuleKey);
        }

    }

    private MagikGrammar forbiddenRuleKey;

    public MagikRuleForbiddenAssert(Rule actual, MagikGrammar forbiddenRuleKey) {
        super(actual);
        this.forbiddenRuleKey = forbiddenRuleKey;
    }

    /**
     * Test if input matches.
     */
    public MagikRuleForbiddenAssert matches(String input) {
        ParsingResult parsingResult = this.parseInput(input);

        // Ensure forbiddenRuleKey was never matched.
        ParseNode parseTree = parsingResult.getParseTreeRoot();
        ParseNode foundTree = this.findParseTreeWithRule(parseTree, this.forbiddenRuleKey);
        if (foundTree != null) {
            throw new ForbiddenRuleUsed(this.forbiddenRuleKey);
        }

        return this;
    }

    public static MagikRuleForbiddenAssert assertThat(Rule actual, MagikGrammar forbiddenRuleKey) {
        return new MagikRuleForbiddenAssert(actual, forbiddenRuleKey);
    }

}
