package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import org.sonar.check.Rule;

/** Check if the number of assigned variables matches the number of returned values. */
@Rule(key = MultipleAssignmentCountMismatchTypedCheck.CHECK_KEY)
public class MultipleAssignmentCountMismatchTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MultipleAssignmentCountMismatch";

  private static final String MESSAGE =
      "Multiple assignment has %d variable(s), but right-hand side returns %d value(s).";

  @Override
  protected void walkPostMultipleAssignmentStatement(final AstNode node) {
    final AstNode assignablesNode =
        node.getFirstChild(MagikGrammar.MULTIPLE_ASSIGNMENT_ASSIGNABLES);
    final AstNode rightNode = node.getLastChild();
    this.checkCountMismatch(assignablesNode, rightNode);
  }

  @Override
  protected void walkPostVariableDefinitionMulti(final AstNode node) {
    final AstNode assignablesNode = node.getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER);
    final AstNode rightNode = node.getFirstChild(MagikGrammar.TUPLE);
    this.checkCountMismatch(assignablesNode, rightNode);
  }

  private void checkCountMismatch(final AstNode assignablesNode, final AstNode rightNode) {
    if (assignablesNode == null || rightNode == null) {
      return;
    }

    // Get right-hand side result type.
    final LocalTypeReasonerState state = this.getTypeReasonerState();
    final ExpressionResultString result = state.getNodeType(rightNode);

    // Cannot determine result count for UNDEFINED results.
    if (result.equals(ExpressionResultString.UNDEFINED)) {
      return;
    }

    final int resultCount = result.size();

    // Get left-hand side variable count.
    final List<AstNode> expressionNodes = assignablesNode.getChildren(MagikGrammar.EXPRESSION);
    final List<AstNode> identifierNodes = assignablesNode.getChildren(MagikGrammar.IDENTIFIER);
    final int variableCount =
        expressionNodes.isEmpty() ? identifierNodes.size() : expressionNodes.size();

    // Check for _gather on the left side — if present, any count is valid.
    final boolean hasGather =
        assignablesNode.getChildren().stream()
            .anyMatch(
                child -> child.getTokenValue().equalsIgnoreCase(MagikKeyword.GATHER.getValue()));
    if (hasGather) {
      return;
    }

    if (variableCount != resultCount) {
      final String message = MESSAGE.formatted(variableCount, resultCount);
      this.addIssue(assignablesNode, message);
    }
  }
}
