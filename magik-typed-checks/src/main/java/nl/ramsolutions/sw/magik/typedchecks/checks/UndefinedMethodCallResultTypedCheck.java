package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Check for UNDEFINED method call result. */
@Rule(key = UndefinedMethodCallResultTypedCheck.CHECK_KEY)
public class UndefinedMethodCallResultTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "UndefinedMethodCallResult";

  private static final String MESSAGE = "UNDEFINED results for method: %s";

  @Override
  @SuppressWarnings("checkstyle:NestedIfDepth")
  protected void walkPostMethodInvocation(final AstNode node) {
    final LocalTypeReasonerState reasonerState = this.getTypeReasonerState();
    final ExpressionResult result = reasonerState.getNodeType(node);
    if (result.containsUndefined()) {
      final AstNode firstIdentifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
      final AstNode issueNode = firstIdentifierNode != null ? firstIdentifierNode : node;

      final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
      final String methodName = helper.getMethodName();

      AbstractType calledType = this.getTypeInvokedOn(node);
      final Collection<Method> methods = calledType.getMethods(methodName);
      if (!methods.isEmpty()) {
        // Set real called type.
        // TODO: Only first method, or should we test all methods?
        final Method method = methods.iterator().next();
        calledType = method.getOwner();
      }
      final String fullMethodName = calledType.getFullName() + "." + methodName;

      final String message = String.format(MESSAGE, fullMethodName);
      this.addIssue(issueNode, message);
    }
  }
}
