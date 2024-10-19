package nl.ramsolutions.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikVisitor;

/** NestingLevel visitor. */
public class NestingLevelVisitor extends MagikVisitor {

  private int nestingLevel = 0;
  private AstNode startingNode;
  private boolean isInForLoop = false;

  public NestingLevelVisitor(AstNode startingNode) {
    this.startingNode = startingNode;
  }

  @Override
  protected void walkPreIf(final AstNode node) {
    this.nestingLevel++;
  }

  @Override
  protected void walkPreFor(final AstNode node) {
    this.nestingLevel++;
    this.isInForLoop = true;
  }

  @Override
  protected void walkPostFor(final AstNode node) {
    this.isInForLoop = false;
  }

  @Override
  protected void walkPreWhile(final AstNode node) {
    this.nestingLevel++;
    this.isInForLoop = true;
  }

  @Override
  protected void walkPreLoop(final AstNode node) {
    if (!this.isInForLoop) {
      this.nestingLevel++;
    }
  }

  public boolean isStartNode(AstNode node) {
    return this.startingNode != null && this.startingNode.equals(node);
  }

  public int getNestingLevel() {
    return this.nestingLevel;
  }
}
