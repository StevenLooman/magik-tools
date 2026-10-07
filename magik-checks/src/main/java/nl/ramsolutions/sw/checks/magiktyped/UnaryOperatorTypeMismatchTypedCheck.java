package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.UnaryOperatorHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import org.sonar.check.Rule;

/** Check if unary operator types are compatible. */
@Rule(key = UnaryOperatorTypeMismatchTypedCheck.CHECK_KEY)
public class UnaryOperatorTypeMismatchTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "UnaryOperatorTypeMismatch";

  private static final String MESSAGE = "Unary operator '%s' ('%s.%s') has no return type defined.";

  @Override
  protected void walkPostUnaryExpression(final AstNode node) {
    final UnaryOperatorHelper helper = new UnaryOperatorHelper(node);
    if (helper.isAllResults()) {
      return;
    }

    if (!helper.isUnaryOperator()) {
      return;
    }

    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final TypeString resultTypeStr = state.getNodeType(node).get(0, TypeString.UNDEFINED);
    if (!resultTypeStr.isUndefined()) {
      return;
    }

    final AstNode operandNode = node.getLastChild();
    final TypeString operandTypeStr = state.getNodeType(operandNode).get(0, TypeString.UNDEFINED);
    if (operandTypeStr.isUndefined()) {
      return;
    }

    final String methodName = helper.getUnaryOperatorMethod();
    // A scatter's values are unknown when the collection's element type is, not its method.
    if (helper.isScatter() && this.hasDefinedReturnType(operandTypeStr, methodName)) {
      return;
    }

    final String message =
        MESSAGE.formatted(node.getTokenValue(), operandTypeStr.getFullString(), methodName);
    this.addIssue(node, message);
  }

  /**
   * Whether a method {@code operandTypeStr} responds to declares a known first result.
   *
   * @param operandTypeStr Type the method is invoked on.
   * @param methodName Name of the method.
   * @return True if a responding method declares a known first result.
   */
  private boolean hasDefinedReturnType(final TypeString operandTypeStr, final String methodName) {
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getRespondingMethodDefinitions(operandTypeStr, methodName);
    return methodDefinitions.stream()
        .map(MethodDefinition::getReturnTypes)
        .map(returnTypes -> returnTypes.get(0, TypeString.UNDEFINED))
        .anyMatch(returnTypeStr -> !returnTypeStr.isUndefined());
  }
}
