package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;

import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

@Rule(key = NoStatementAfterBodyExitCheck.CHECK_KEY)
public class NoStatementAfterBodyExitCheck extends MagikCheck {
  public static final String CHECK_KEY = "NoStatementAfterBodyExit";
  private static final String MESSAGE = "Statement is never executed.";

  @Override
  protected void walkPreReturnStatement(AstNode node) {
    checkNoNextStatements(node);
  }

  @Override
  protected void walkPreEmitStatement(AstNode node) {
    checkNoNextStatements(node);
  }

  @Override
  protected void walkPreLeaveStatement(AstNode node) {
    checkNoNextStatements(node);
  }

  @Override
  protected void walkPreContinueStatement(AstNode node) {
    checkNoNextStatements(node);
  }

  private void checkNoNextStatements(AstNode node) {
    AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    AstNode nextStatementNode = statementNode.getNextSibling();
    while (nextStatementNode != null) {
      if (nextStatementNode.getType() instanceof MagikGrammar) {
        addIssue(MESSAGE, nextStatementNode);
      }

      nextStatementNode = nextStatementNode.getNextSibling();
    }
  }

}
