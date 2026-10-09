package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/**
 * Check for an assignment to something that cannot be assigned to, such as a procedure call or a
 * literal.
 *
 * <p>A target is a variable, a slot, or a parenthesized target. A method call is never reported:
 * {@code a.x << 1} and {@code a[1] << 1} are method invocations, and {@code a.x +<< 1} reads and
 * writes through them.
 */
@Rule(key = InvalidAssignmentTargetCheck.CHECK_KEY)
public class InvalidAssignmentTargetCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "InvalidAssignmentTarget";

  private static final String MESSAGE = "This cannot be assigned to.";

  @Override
  protected void walkPreAssignmentExpression(final AstNode node) {
    this.reportInvalidTargets(node);
  }

  @Override
  protected void walkPreAugmentedAssignmentExpression(final AstNode node) {
    this.reportInvalidTargets(node);
  }

  @Override
  protected void walkPreMultipleAssignmentAssignables(final AstNode node) {
    final List<AstNode> targetNodes = node.getChildren(MagikGrammar.EXPRESSION);
    targetNodes.forEach(this::reportInvalidTarget);
  }

  private void reportInvalidTargets(final AstNode node) {
    // Every operand but the last is assigned to.
    final List<AstNode> operandNodes = node.getChildren(MagikGrammar.values());
    final int targetCount = operandNodes.size() - 1;
    final List<AstNode> targetNodes = operandNodes.subList(0, targetCount);
    targetNodes.forEach(this::reportInvalidTarget);
  }

  private void reportInvalidTarget(final AstNode node) {
    if (!this.isAssignable(node)) {
      this.addIssue(node, MESSAGE);
    }
  }

  private boolean isAssignable(final AstNode node) {
    if (node.is(MagikGrammar.EXPRESSION)) {
      final AstNode childNode = node.getFirstChild();
      return this.isAssignable(childNode);
    }

    if (node.is(MagikGrammar.POSTFIX_EXPRESSION)) {
      final AstNode lastChildNode = node.getLastChild();
      return lastChildNode.is(MagikGrammar.METHOD_INVOCATION);
    }

    if (!node.is(MagikGrammar.ATOM)) {
      return false;
    }

    if (node.hasDirectChildren(MagikGrammar.IDENTIFIER, MagikGrammar.SLOT)) {
      return true;
    }

    // A parenthesized target; a tuple is not one.
    final List<AstNode> expressionNodes = node.getChildren(MagikGrammar.EXPRESSION);
    return expressionNodes.size() == 1 && this.isAssignable(expressionNodes.get(0));
  }
}
