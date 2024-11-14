package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check for forbidden calls. */
@Rule(key = ForbiddenCallCheck.CHECK_KEY)
public class ForbiddenCallCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ForbiddenCall";

  private static final String MESSAGE = "Call '%s' is forbidden.";
  private static final String DEFAULT_FORBIDDEN_CALLS =
      "show(), sw:show(), print(), sw:print(), debug_print(), sw:debug_print(), .sys!perform(), .sys!slot()";

  /** List of forbidden calls, separated by ','. */
  @RuleProperty(
      key = "forbidden calls",
      defaultValue = "" + DEFAULT_FORBIDDEN_CALLS,
      description = "List of forbidden calls, separated by ','",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String forbiddenCalls = DEFAULT_FORBIDDEN_CALLS;

  private Set<String> getForbiddenCalls() {
    return Arrays.stream(this.forbiddenCalls.split(","))
        .map(String::trim)
        .collect(Collectors.toSet());
  }

  @Override
  protected void walkPreMethodInvocation(final AstNode node) {
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    if (!this.getForbiddenCalls().contains("." + methodName)) {
      return;
    }

    final AstNode methodNameNode = helper.getMethodNameNode();
    final String message = String.format(MESSAGE, methodName);
    this.addIssue(methodNameNode, message);
  }

  @Override
  protected void walkPreProcedureInvocation(final AstNode node) {
    final AstNode parentNode = node.getParent();
    if (!parentNode.is(MagikGrammar.POSTFIX_EXPRESSION)) {
      return;
    }

    final String identifier = parentNode.getTokenValue() + "()";
    if (!this.getForbiddenCalls().contains(identifier)) {
      return;
    }

    final String message = String.format(MESSAGE, identifier);
    this.addIssue(parentNode, message);
  }
}
