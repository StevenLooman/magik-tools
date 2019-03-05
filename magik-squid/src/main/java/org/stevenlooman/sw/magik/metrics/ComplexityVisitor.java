package org.stevenlooman.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;

import org.stevenlooman.sw.magik.MagikVisitor;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.List;

/**
 * Complexity visitor for Magik.
 */
public class ComplexityVisitor extends MagikVisitor {
  private int complexity = 1;

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.PROC_DEFINITION,
        MagikGrammar.LOOP,
        MagikGrammar.IF,
        MagikGrammar.ELIF,
        MagikGrammar.AND_EXPRESSION,
        MagikGrammar.OR_EXPRESSION);
  }

  @Override
  public void visitNode(AstNode ast) {
    complexity++;
  }

  public int getComplexity() {
    return complexity;
  }

}
