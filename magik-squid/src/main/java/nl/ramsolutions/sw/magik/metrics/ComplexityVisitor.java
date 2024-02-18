package nl.ramsolutions.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikVisitor;

/** Complexity visitor. */
public class ComplexityVisitor extends MagikVisitor {

  private int complexity = 1;

  @Override
  protected void walkPreProcedureDefinition(final AstNode node) {
    this.complexity++;
  }

  @Override
  protected void walkPreLoop(final AstNode node) {
    this.complexity++;
  }

  @Override
  protected void walkPreIf(final AstNode node) {
    this.complexity++;
  }

  @Override
  protected void walkPreElif(final AstNode node) {
    this.complexity++;
  }

  @Override
  protected void walkPreAndExpression(final AstNode node) {
    this.complexity++;
  }

  @Override
  protected void walkPreOrExpression(final AstNode node) {
    this.complexity++;
  }

  public int getComplexity() {
    return this.complexity;
  }
}
