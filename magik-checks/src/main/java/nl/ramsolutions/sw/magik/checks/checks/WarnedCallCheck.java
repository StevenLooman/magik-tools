package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check for warned calls. */
@Rule(key = WarnedCallCheck.CHECK_KEY)
public class WarnedCallCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "WarnedCall";

  private static final String MESSAGE = "Call is warned.";
  private static final String DEFAULT_WARNED_CALLS = "write(),sw:write()";

  /** List of Warned calls, separated by ','. */
  @RuleProperty(
      key = "warned calls",
      defaultValue = "" + DEFAULT_WARNED_CALLS,
      description = "List of Warned calls, separated by ','",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String warnedCalls = DEFAULT_WARNED_CALLS;

  private Set<String> getWarnedCalls() {
    return Arrays.stream(this.warnedCalls.split(",")).map(String::trim).collect(Collectors.toSet());
  }

  @Override
  protected void walkPreMethodInvocation(final AstNode node) {
    final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
    if (identifierNode == null) {
      return;
    }

    final String identifier = "." + identifierNode.getTokenValue();
    if (!getWarnedCalls().contains(identifier)) {
      return;
    }

    this.addIssue(node, MESSAGE);
  }

  @Override
  protected void walkPreProcedureInvocation(final AstNode node) {
    final AstNode parentNode = node.getParent();
    if (!parentNode.is(MagikGrammar.POSTFIX_EXPRESSION)) {
      return;
    }

    final String identifier = parentNode.getTokenValue() + "()";
    if (!getWarnedCalls().contains(identifier)) {
      return;
    }

    this.addIssue(node, MESSAGE);
  }
}
