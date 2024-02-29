package nl.ramsolutions.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikVisitor;

/** Statement-count visitor. */
public class StatementCountVisitor extends MagikVisitor {

  private int statementCount;

  @Override
  protected void walkPreMagik(final AstNode node) {
    this.statementCount = 0;
  }

  @Override
  public void walkPreStatement(final AstNode ast) {
    this.statementCount++;
  }

  public int getStatementCount() {
    return this.statementCount;
  }
}
