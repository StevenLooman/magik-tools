package org.stevenlooman.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.stevenlooman.sw.magik.MagikVisitor;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.List;

public class StatementCountVisitor extends MagikVisitor {
  private int statementCount = 0;

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(MagikGrammar.STATEMENT);
  }

  @Override
  public void visitNode(AstNode ast) {
    statementCount++;
  }

  public int getStatementCount() {
    return statementCount;
  }

}
