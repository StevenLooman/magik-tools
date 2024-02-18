package nl.ramsolutions.sw.magik.api;

import com.sonar.sslr.api.Rule;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.internal.grammar.MutableParsingRule;
import org.sonar.sslr.internal.matchers.Matcher;
import org.sonar.sslr.internal.matchers.ParseNode;
import org.sonar.sslr.internal.vm.EndOfInputExpression;
import org.sonar.sslr.parser.ParseErrorFormatter;
import org.sonar.sslr.parser.ParseRunner;
import org.sonar.sslr.parser.ParsingResult;
import org.sonar.sslr.tests.ParsingResultComparisonFailure;
import org.sonar.sslr.tests.RuleAssert;

/** Magik Rule assert base class. */
public abstract class MagikRuleAssert extends RuleAssert {

  /** With end of input GammarRuleKey. */
  static class WithEndOfInput implements GrammarRuleKey {

    private final GrammarRuleKey ruleKey;

    WithEndOfInput(final GrammarRuleKey ruleKey) {
      this.ruleKey = ruleKey;
    }

    @Override
    public String toString() {
      return this.ruleKey + " with end of input";
    }
  }

  /** End of input GrammarRuleKey. */
  static class EndOfInput implements GrammarRuleKey {

    @Override
    public String toString() {
      return "end of input";
    }
  }

  protected MagikRuleAssert(final Rule actual) {
    super(actual);
  }

  /**
   * Create ParseRunner with WithEndOfInput matcher.
   *
   * @return
   */
  protected ParseRunner createParseRunnerWithEofMatcher() {
    this.isNotNull();

    final MutableParsingRule rule = (MutableParsingRule) actual;
    final MutableParsingRule endOfInput =
        (MutableParsingRule)
            new MutableParsingRule(new EndOfInput()).is(EndOfInputExpression.INSTANCE);
    final MutableParsingRule withEndOfInput =
        (MutableParsingRule)
            new MutableParsingRule(new WithEndOfInput(rule.getRuleKey())).is(actual, endOfInput);
    return new ParseRunner(withEndOfInput);
  }

  /** Get rule name. */
  protected String getRuleName() {
    final MutableParsingRule actualRule = (MutableParsingRule) this.actual;
    return actualRule.getName();
  }

  /** Parse input. */
  protected ParsingResult parseInput(final String input) {
    final ParseRunner parseRunner = createParseRunnerWithEofMatcher();
    final ParsingResult parsingResult = parseRunner.parse(input.toCharArray());
    if (!parsingResult.isMatched()) {
      final String expected = "Rule '" + getRuleName() + "' should match:\n" + input;
      final String actual = new ParseErrorFormatter().format(parsingResult.getParseError());
      throw new ParsingResultComparisonFailure(expected, actual);
    }

    return parsingResult;
  }

  /** Find parse tree with a given MagikGrammar RuleKey. */
  @CheckForNull
  protected ParseNode findParseTreeWithRule(
      final ParseNode parseNode, final MagikGrammar findRuleKey) {
    final Matcher matcher = parseNode.getMatcher();
    if (matcher instanceof MutableParsingRule) {
      final MutableParsingRule parsingRule = (MutableParsingRule) matcher;
      final GrammarRuleKey ruleKey = parsingRule.getRuleKey();
      if (ruleKey == findRuleKey) {
        return parseNode;
      }
    }

    for (final ParseNode child : parseNode.getChildren()) {
      final ParseNode childParseNode = this.findParseTreeWithRule(child, findRuleKey);
      if (childParseNode != null) {
        return childParseNode;
      }
    }

    return null;
  }
}
