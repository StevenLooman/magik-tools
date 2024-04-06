package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Test to check if a CONDITIONAL_EXPRESSION results in a _false type. */
@Rule(key = ConditionalExpressionIsFalseTypedCheck.CHECK_KEY)
public class ConditionalExpressionIsFalseTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ConditionalExpressionIsFalse";

  private static final String MESSAGE =
      "Conditional expression does not result in true/false, but '%s'";

  @Override
  protected void walkPostConditionalExpression(final AstNode node) {
    final LocalTypeReasonerState reasonerState = this.getTypeReasonerState();
    final AstNode expressionNode = node.getFirstChild(MagikGrammar.EXPRESSION);
    final ExpressionResultString result = reasonerState.getNodeType(expressionNode);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    if (typeStr.isUndefined()
        || typeStr.isCombined() && typeStr.getCombinedTypes().contains(TypeString.UNDEFINED)) {
      return;
    }

    if (!typeStr.equals(TypeString.SW_FALSE)) {
      final String typeStringStr = typeStr.getFullString();
      final String message = String.format(MESSAGE, typeStringStr);
      this.addIssue(node, message);
    }
  }
}
