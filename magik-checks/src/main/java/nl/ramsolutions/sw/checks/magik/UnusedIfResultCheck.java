package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check if result of an _if-expression that emits a value is consumed. */
@Rule(key = UnusedIfResultCheck.CHECK_KEY)
public class UnusedIfResultCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "UnusedIfResult";

  private static final String MESSAGE = "Result of _if expression is not used.";

  @Override
  protected void walkPreIf(final AstNode node) {
    if (!ifProducesValue(node)) {
      return;
    }
    if (!isStandaloneIfStatement(node)) {
      return;
    }
    this.addIssue(node, MESSAGE);
  }

  private boolean ifProducesValue(final AstNode ifNode) {
    // Check _then body
    final AstNode thenBody = ifNode.getFirstChild(MagikGrammar.BODY);
    if (thenBody != null && bodyHasEmit(thenBody)) {
      return true;
    }
    // Check each _elif body
    for (final AstNode elif : ifNode.getChildren(MagikGrammar.ELIF)) {
      final AstNode elifBody = elif.getFirstChild(MagikGrammar.BODY);
      if (elifBody != null && bodyHasEmit(elifBody)) {
        return true;
      }
    }
    // Check _else body
    final AstNode elseNode = ifNode.getFirstChild(MagikGrammar.ELSE);
    if (elseNode != null) {
      final AstNode elseBody = elseNode.getFirstChild(MagikGrammar.BODY);
      if (elseBody != null && bodyHasEmit(elseBody)) {
        return true;
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

  /**
   * Returns true when the IF node is a standalone expression statement whose result is discarded.
   *
   * <p>When standalone, the SSLR grammar compacts the path to: IF → ATOM → EXPRESSION →
   * EXPRESSION_STATEMENT. Any consuming context (assignment, emit, return, method argument) breaks
   * this exact chain before EXPRESSION_STATEMENT is reached.
   */
  private boolean isStandaloneIfStatement(final AstNode ifNode) {
    final AstNode atom = ifNode.getParent();
    if (atom == null || !atom.is(MagikGrammar.ATOM)) {
      return false;
    }
    final AstNode expression = atom.getParent();
    if (expression == null || !expression.is(MagikGrammar.EXPRESSION)) {
      return false;
    }
    final AstNode expressionStatement = expression.getParent();
    return expressionStatement != null
        && expressionStatement.is(MagikGrammar.EXPRESSION_STATEMENT);
  }
}
