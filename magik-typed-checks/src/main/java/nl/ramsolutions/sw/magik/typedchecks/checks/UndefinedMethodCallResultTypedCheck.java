package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
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
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final LocalTypeReasonerState reasonerState = this.getTypeReasonerState();
    final TypeString receiverTypeStr = this.getTypeInvokedOn(node);
    if (receiverTypeStr.isUndefined()) {
      // Don't bother with method invocations on UNDEFINED.
      return;
    }

    final String methodName = helper.getMethodName();
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getMethodDefinitions(receiverTypeStr, methodName);
    if (methodDefinitions.isEmpty()) {
      // Don't bother with unknown methods.
      return;
    }

    final ExpressionResultString result = reasonerState.getNodeType(node);
    final boolean containsUndefined = result.stream().anyMatch(TypeString::containsUndefined);
    if (containsUndefined) {
      final AstNode firstIdentifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
      final AstNode issueNode = firstIdentifierNode != null ? firstIdentifierNode : node;
      final String fullMethodName = receiverTypeStr.getFullString() + "." + methodName;
      final String message = String.format(MESSAGE, fullMethodName);
      this.addIssue(issueNode, message);
    }
  }
}
