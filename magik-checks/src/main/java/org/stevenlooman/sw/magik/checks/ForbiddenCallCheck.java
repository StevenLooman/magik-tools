package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = ForbiddenCallCheck.CHECK_KEY)
public class ForbiddenCallCheck extends MagikCheck {
  public static final String CHECK_KEY = "ForbiddenCall";
  private static final String MESSAGE = "Call is forbidden.";
  private static final String DEFAULT_FORBIDDEN_CALLS = "show(), print(), debug_print()";

  @RuleProperty(
      key = "forbidden calls",
      defaultValue = "" + DEFAULT_FORBIDDEN_CALLS,
      description = "List of forbidden calls, separated by ','")
  public String forbiddenCalls = DEFAULT_FORBIDDEN_CALLS;

  private Set<String> getForbiddenCalls() {
    return Arrays.stream(forbiddenCalls.split(","))
        .map(s -> s.trim())
        .collect(Collectors.toSet());
  }

  @Override
  protected void walkPreMethodInvocation(AstNode node) {
    AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
    if (identifierNode == null) {
      return;
    }

    String identifier = "." + identifierNode.getTokenValue();
    if (!getForbiddenCalls().contains(identifier)) {
      return;
    }

    addIssue(MESSAGE, node);
  }

  @Override
  protected void walkPreProcedureInvocation(AstNode node) {
    AstNode parentNode = node.getParent();
    if (parentNode.getType() != MagikGrammar.POSTFIX_EXPRESSION) {
      return;
    }

    String identifier = parentNode.getTokenValue() + "()";
    if (!getForbiddenCalls().contains(identifier)) {
      return;
    }

    addIssue(MESSAGE, node);
  }

}
