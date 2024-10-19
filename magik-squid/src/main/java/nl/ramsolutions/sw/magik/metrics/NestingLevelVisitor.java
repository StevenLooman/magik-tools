package nl.ramsolutions.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikVisitor;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** NestingLevel visitor. */
public class NestingLevelVisitor extends MagikVisitor {
  private int nestingLevel = 0;
  private AstNode startingNode;

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
  }

  @Override
  protected void walkPreWhile(final AstNode node) {
    this.nestingLevel++;
  }

  @Override
  protected void walkPreLoop(final AstNode node) {
    AstNode parent = node.getParent();
    if (parent == null) {
      this.nestingLevel++;
    } else if (parent.is(MagikGrammar.OVER)) {
      AstNode grandParent = parent.getParent();
      if (grandParent == null || !grandParent.is(MagikGrammar.FOR)) {
        this.nestingLevel++;
      }
    } else if (!parent.is(MagikGrammar.FOR, MagikGrammar.WHILE)) {
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
