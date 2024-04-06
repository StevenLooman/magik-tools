package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Test if `sw:char16_vector.evaluate()` is used. */
@Rule(key = SwChar16VectorEvaluateInvocationCheck.CHECK_KEY)
public class SwChar16VectorEvaluateInvocationCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "SwChar16VectorEvaluateInvocation";

  private static final String METHOD_NAME = "evaluate()";
  private static final String MESSAGE =
      "Method '"
          + TypeString.SW_CHAR16_VECTOR.getFullString()
          + "."
          + METHOD_NAME
          + "' is unsafe to use.";

  @Override
  protected void walkPreMethodInvocation(final AstNode node) {
    final TypeString receiverTypeStr = this.getTypeInvokedOn(node).getWithoutGenerics();
    if (!receiverTypeStr.equals(TypeString.SW_CHAR16_VECTOR)) {
      return;
    }

    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    if (!methodName.equals(METHOD_NAME)) {
      return;
    }

    this.addIssue(node, MESSAGE);
  }
}
