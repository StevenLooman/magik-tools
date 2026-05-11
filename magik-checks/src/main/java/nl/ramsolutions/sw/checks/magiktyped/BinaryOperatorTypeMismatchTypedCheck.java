package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import org.sonar.check.Rule;

/** Check if binary operator types are compatible. */
@Rule(key = BinaryOperatorTypeMismatchTypedCheck.CHECK_KEY)
public class BinaryOperatorTypeMismatchTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "BinaryOperatorTypeMismatch";

  private static final String AUGMENTED_ASSIGNMENT_MESSAGE =
      "Augmented assignment operator '%s' results in an undefined type.";
  private static final String BINARY_OPERATOR_MESSAGE =
      "Binary operator '%s' results in an undefined type.";

  @Override
  protected void walkPostAugmentedAssignmentExpression(final AstNode node) {
    this.reportAugmentedAssignmentMismatch(node);
  }

  @Override
  protected void walkPostOrExpression(final AstNode node) {
    this.reportBinaryExpressionMismatch(node);
  }

  @Override
  protected void walkPostXorExpression(final AstNode node) {
    this.reportBinaryExpressionMismatch(node);
  }

  @Override
  protected void walkPostAndExpression(final AstNode node) {
    this.reportBinaryExpressionMismatch(node);
  }

  @Override
  protected void walkPostEqualityExpression(final AstNode node) {
    this.reportBinaryExpressionMismatch(node);
  }

  @Override
  protected void walkPostRelationalExpression(final AstNode node) {
    this.reportBinaryExpressionMismatch(node);
  }

  @Override
  protected void walkPostAdditiveExpression(final AstNode node) {
    this.reportBinaryExpressionMismatch(node);
  }

  @Override
  protected void walkPostMultiplicativeExpression(final AstNode node) {
    this.reportBinaryExpressionMismatch(node);
  }

  @Override
  protected void walkPostExponentialExpression(final AstNode node) {
    this.reportBinaryExpressionMismatch(node);
  }

  private void reportAugmentedAssignmentMismatch(final AstNode node) {
    final LocalTypeReasonerState state = this.getTypeReasonerState();

    final ExpressionResultString expressionResult = state.getNodeType(node);
    final TypeString resultTypeStr = expressionResult.get(0, TypeString.UNDEFINED);
    if (!resultTypeStr.isUndefined()) {
      return;
    }

    final AstNode rightNode = node.getLastChild();
    final ExpressionResultString rightResult = state.getNodeType(rightNode);
    final TypeString rightTypeStr = rightResult.get(0, TypeString.UNDEFINED);
    if (rightTypeStr.isUndefined()) {
      return;
    }

    final AstNode operatorNode = node.getChildren().get(1);
    final String operatorStr = operatorNode.getTokenValue().toLowerCase();
    if (this.isIgnoredOperator(operatorStr)) {
      return;
    }

    final String message = AUGMENTED_ASSIGNMENT_MESSAGE.formatted(operatorNode.getTokenValue());
    this.addIssue(operatorNode, message);
  }

  private void reportBinaryExpressionMismatch(final AstNode node) {
    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final TypeString expressionTypeStr = state.getNodeType(node).get(0, TypeString.UNDEFINED);
    if (!expressionTypeStr.isUndefined()) {
      return;
    }

    TypeString leftTypeStr = state.getNodeType(node.getFirstChild()).get(0, TypeString.UNDEFINED);
    if (leftTypeStr.isUndefined()) {
      return;
    }

    final int childCount = node.getChildren().size();
    for (int i = 1; i < childCount - 1; i += 2) {
      final AstNode operatorNode = node.getChildren().get(i);
      final String operatorStr = operatorNode.getTokenValue().toLowerCase();
      if (this.isIgnoredOperator(operatorStr)) {
        return;
      }

      final AstNode rightNode = node.getChildren().get(i + 1);
      final TypeString rightTypeStr = state.getNodeType(rightNode).get(0, TypeString.UNDEFINED);
      if (rightTypeStr.isUndefined()) {
        return;
      }

      leftTypeStr = this.resolveBinaryResultType(operatorStr, leftTypeStr, rightTypeStr);
      if (leftTypeStr.isUndefined()) {
        final String message = BINARY_OPERATOR_MESSAGE.formatted(operatorNode.getTokenValue());
        this.addIssue(operatorNode, message);
        return;
      }
    }
  }

  private boolean isIgnoredOperator(final String operatorStr) {
    return MagikKeyword.IS.getValue().equals(operatorStr)
        || MagikKeyword.ISNT.getValue().equals(operatorStr)
        || MagikKeyword.ANDIF.getValue().equals(operatorStr)
        || MagikKeyword.ORIF.getValue().equals(operatorStr);
  }

  private TypeString resolveBinaryResultType(
      final String operatorStr, final TypeString leftTypeStr, final TypeString rightTypeStr) {
    return this.getDefinitionKeeper()
        .getBinaryOperatorDefinitions(operatorStr, leftTypeStr, rightTypeStr)
        .stream()
        .map(BinaryOperatorDefinition::getResultTypeName)
        .reduce(TypeString::combine)
        .orElse(TypeString.UNDEFINED);
  }
}
