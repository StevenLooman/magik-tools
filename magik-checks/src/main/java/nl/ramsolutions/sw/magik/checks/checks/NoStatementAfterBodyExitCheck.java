package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/** Check for statements after _return/>>/_leave/_continue. */
@Rule(key = NoStatementAfterBodyExitCheck.CHECK_KEY)
public class NoStatementAfterBodyExitCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "NoStatementAfterBodyExit";

  private static final String MESSAGE = "Statement is never executed.";

  @Override
  protected void walkPreReturnStatement(final AstNode node) {
    this.checkNoNextStatements(node);
  }

  @Override
  protected void walkPreEmitStatement(final AstNode node) {
    this.checkNoNextStatements(node);
  }

  @Override
  protected void walkPreLeaveStatement(final AstNode node) {
    this.checkNoNextStatements(node);
  }

  @Override
  protected void walkPreContinueStatement(final AstNode node) {
    this.checkNoNextStatements(node);
  }

  private void checkNoNextStatements(final AstNode node) {
    final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    AstNode nextStatementNode = statementNode.getNextSibling();
    while (nextStatementNode != null) {
      if (nextStatementNode.is(MagikGrammar.values())) {
        this.addIssue(nextStatementNode, MESSAGE);
      }

      nextStatementNode = nextStatementNode.getNextSibling();
    }
  }
}
