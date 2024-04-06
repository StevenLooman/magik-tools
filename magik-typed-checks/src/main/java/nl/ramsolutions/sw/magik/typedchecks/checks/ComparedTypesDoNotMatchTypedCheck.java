package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Test if compared type can match. */
@Rule(key = ComparedTypesDoNotMatchTypedCheck.CHECK_KEY)
public class ComparedTypesDoNotMatchTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ComparedTypesDoNotMatch";

  private static final String IS_KIND_OF = "is_kind_of?()";
  private static final String IS_CLASS_OF = "is_class_of?()";
  private static final String MESSAGE = "Left type (%s) can never match right type (%s)";

  @Override
  protected void walkPostEqualityExpression(final AstNode node) {
    final List<AstNode> children = node.getChildren();
    final AstNode operatorNode = children.get(1);
    if (!operatorNode.getTokenValue().equalsIgnoreCase("_is")
        && !operatorNode.getTokenValue().equalsIgnoreCase("_isnt")) {
      return;
    }

    final AstNode leftNode = children.get(0);
    final AstNode rightNode = children.get(2);

    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final TypeString leftTypeStr = state.getNodeType(leftNode).get(0, TypeString.UNDEFINED);
    final TypeString rightTypeStr = state.getNodeType(rightNode).get(0, TypeString.UNDEFINED);
    if (leftTypeStr.isUndefined() || rightTypeStr.isUndefined()) {
      return;
    }

    final TypeString intersection = TypeString.intersection(leftTypeStr, rightTypeStr);
    if (intersection == null) {
      final String message =
          String.format(MESSAGE, leftTypeStr.getFullString(), rightTypeStr.getFullString());
      this.addIssue(node, message);
    }
  }

  @SuppressWarnings("checkstyle:NestedIfDepth")
  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final boolean isKindOf =
        helper.isMethodInvocationOf(ComparedTypesDoNotMatchTypedCheck.IS_KIND_OF);
    final boolean isClassOf =
        helper.isMethodInvocationOf(ComparedTypesDoNotMatchTypedCheck.IS_CLASS_OF);
    if (isKindOf || isClassOf) {
      final LocalTypeReasonerState state = this.getTypeReasonerState();
      final AstNode receiverNode = helper.getReceiverNode();
      final TypeString receiverTypeStr =
          state.getNodeType(receiverNode).get(0, TypeString.UNDEFINED);
      if (receiverTypeStr.isUndefined()) {
        return;
      }

      final List<AstNode> argumentNodes = helper.getArgumentExpressionNodes();
      final AstNode argument0Node = argumentNodes.get(0);
      final ExpressionResultString argument0Result = state.getNodeType(argument0Node);
      final TypeString checkedTypeStr = argument0Result.get(0, TypeString.UNDEFINED);
      if (checkedTypeStr.isUndefined()) {
        return;
      }

      if (isClassOf) {
        final TypeString intersection = TypeString.intersection(receiverTypeStr, checkedTypeStr);
        if (intersection == null) {
          final String message =
              String.format(
                  MESSAGE, receiverTypeStr.getFullString(), checkedTypeStr.getFullString());
          this.addIssue(node, message);
        }
      } else { // isKindOf.
        final TypeStringResolver resolver = this.getTypeStringResolver();
        if (!resolver.isKindOf(receiverTypeStr, checkedTypeStr)) {
          final String message =
              String.format(
                  MESSAGE, receiverTypeStr.getFullString(), checkedTypeStr.getFullString());
          this.addIssue(node, message);
        }
      }
    }
  }
}
