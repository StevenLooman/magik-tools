package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check if result of a block-like expression that emits a value is consumed. */
@Rule(key = UnusedExpressionResultCheck.CHECK_KEY)
public class UnusedExpressionResultCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "UnusedExpressionResult";

  private static final String MESSAGE = "Result of expression is not used.";

  @Override
  protected void walkPreAtom(final AstNode node) {
    // Must be directly under EXPRESSION → EXPRESSION_STATEMENT (standalone context).
    // SSLR collapses single-child expression-hierarchy nodes, so the path for a standalone
    // construct is exactly: CONSTRUCT → ATOM → EXPRESSION → EXPRESSION_STATEMENT.
    // Any consuming context (assignment, emit, return, method argument, binary op) places
    // a different node between EXPRESSION and EXPRESSION_STATEMENT.
    final AstNode expression = node.getParent();
    if (expression == null || !expression.is(MagikGrammar.EXPRESSION)) {
      return;
    }
    final AstNode expressionStatement = expression.getParent();
    if (expressionStatement == null || !expressionStatement.is(MagikGrammar.EXPRESSION_STATEMENT)) {
      return;
    }
    final AstNode construct = node.getFirstChild();
    if (construct == null || !isBlockLikeConstruct(construct)) {
      return;
    }
    if (constructProducesValue(construct)) {
      this.addIssue(construct, MESSAGE);
    }
  }

  private boolean isBlockLikeConstruct(final AstNode node) {
    return node.is(
        MagikGrammar.IF,
        MagikGrammar.FOR,
        MagikGrammar.OVER,
        MagikGrammar.WHILE,
        MagikGrammar.LOOP,
        MagikGrammar.BLOCK,
        MagikGrammar.PROTECT,
        MagikGrammar.TRY,
        MagikGrammar.CATCH,
        MagikGrammar.LOCK);
  }

  /**
   * Returns true when any directly-reachable body of the construct contains an emit statement.
   * Recurses through structural connector nodes (ELIF, ELSE, WHEN, PROTECTION, LOOP, FINALLY, OVER)
   * but stops at nested constructs so inner expressions are not traversed.
   */
  private boolean constructProducesValue(final AstNode node) {
    for (final AstNode child : node.getChildren()) {
      if (child.is(MagikGrammar.BODY)) {
        if (bodyHasEmit(child)) {
          return true;
        }
      } else if (child.is(
          MagikGrammar.ELIF,
          MagikGrammar.ELSE,
          MagikGrammar.WHEN,
          MagikGrammar.PROTECTION,
          MagikGrammar.LOOP,
          MagikGrammar.FINALLY,
          MagikGrammar.OVER)) {
        if (constructProducesValue(child)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean bodyHasEmit(final AstNode bodyNode) {
    final List<AstNode> emitNodes =
        AstQuery.getChildrenFromChain(
            bodyNode, MagikGrammar.STATEMENT, MagikGrammar.EMIT_STATEMENT);
    return !emitNodes.isEmpty();
  }
}
