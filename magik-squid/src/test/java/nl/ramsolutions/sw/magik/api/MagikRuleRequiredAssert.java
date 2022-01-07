package nl.ramsolutions.sw.magik.api;

import com.sonar.sslr.api.Rule;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.internal.matchers.ParseNode;
import org.sonar.sslr.parser.ParsingResult;

/**
 * Require a rule to have matched.
 */
public class MagikRuleRequiredAssert extends MagikRuleAssert {

    /**
     * Required rule unused assertion error.
     */
    static class RequiredRuleNotUsed extends AssertionError {

        private static final long serialVersionUID = 1L;

        RequiredRuleNotUsed(GrammarRuleKey grammarRuleKey) {
            super("Required rule was not used: " + grammarRuleKey);
        }

    }

    private MagikGrammar requiredRuleKey;

    public MagikRuleRequiredAssert(Rule actual, MagikGrammar requiredRuleKey) {
        super(actual);
        this.requiredRuleKey = requiredRuleKey;
    }

    /**
     * Test if input matches.
     */
    public MagikRuleRequiredAssert matches(String input) {
        ParsingResult parsingResult = this.parseInput(input);

        // Ensure requiredRuleKey was matched.
        ParseNode parseTree = parsingResult.getParseTreeRoot();
        ParseNode foundTree = this.findParseTreeWithRule(parseTree, this.requiredRuleKey);
        if (foundTree == null) {
            throw new RequiredRuleNotUsed(this.requiredRuleKey);
        }

        return this;
    }

    public static MagikRuleRequiredAssert assertThat(Rule actual, MagikGrammar requiredRuleKey) {
        return new MagikRuleRequiredAssert(actual, requiredRuleKey);
    }

}
