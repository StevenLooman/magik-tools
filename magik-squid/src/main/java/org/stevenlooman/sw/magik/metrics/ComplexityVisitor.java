package org.stevenlooman.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;

import org.stevenlooman.sw.magik.MagikVisitor;

/**
 * Complexity visitor for Magik.
 */
public class ComplexityVisitor extends MagikVisitor {
  private int complexity = 1;

  @Override
  protected void walkPreProcDefinition(AstNode node) {
    complexity++;
  }

  @Override
  protected void walkPreLoop(AstNode node) {
    complexity++;
  }

  @Override
  protected void walkPreIf(AstNode node) {
    complexity++;
  }

  @Override
  protected void walkPreElif(AstNode node) {
    complexity++;
  }

  @Override
  protected void walkPreAndExpression(AstNode node) {
    complexity++;
  }

  @Override
  protected void walkPreOrExpression(AstNode node) {
    complexity++;
  }

  public int getComplexity() {
    return complexity;
  }

}
