package org.stevenlooman.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import org.stevenlooman.sw.magik.MagikVisitor;

public class StatementCountVisitor extends MagikVisitor {
  private int statementCount = 0;

  @Override
  public void walkPreStatement(AstNode ast) {
    statementCount++;
  }

  public int getStatementCount() {
    return statementCount;
  }

}
