package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.UnaryOperatorHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
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
    final String message =
        MESSAGE.formatted(node.getTokenValue(), operandTypeStr.getFullString(), methodName);
    this.addIssue(node, message);
  }
}
