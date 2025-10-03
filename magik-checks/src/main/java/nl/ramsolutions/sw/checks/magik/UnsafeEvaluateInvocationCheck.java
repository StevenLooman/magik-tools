package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import org.sonar.check.Rule;

/** Check for `unsafe_evaluate()` method invocations. */
@Rule(key = UnsafeEvaluateInvocationCheck.CHECK_KEY)
public class UnsafeEvaluateInvocationCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "UnsafeEvaluateInvocation";

  private static final String METHOD_NAME = "unsafe_evaluate()";
  private static final String MESSAGE = "Method '" + METHOD_NAME + "' is unsafe to use.";

  @Override
  protected void walkPreMethodInvocation(final AstNode node) {
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    if (!methodName.equals(METHOD_NAME)) {
      return;
    }

    final AstNode methodNameNode = helper.getMethodNameNode();
    this.addIssue(methodNameNode, MESSAGE);
  }
}
