package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.metrics.NestingLevelVisitor;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check nesting level of nodes. */
@Rule(key = NestingLevelCheck.CHECK_KEY)
public class NestingLevelCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "NestingLevel";

  private static final int DEFAULT_MAXIMUM_NESTING_LEVEL = 3;
  private static final String MESSAGE = "Nesting level greater than permitted (%s/%s).";

  /** Maximum nesting level of node. */
  @RuleProperty(
      key = "maximum nesting level",
      defaultValue = "" + DEFAULT_MAXIMUM_NESTING_LEVEL,
      description = "Maximum nesting level",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int maximumNestingLevel = DEFAULT_MAXIMUM_NESTING_LEVEL;

  @Override
  protected void walkPreIf(final AstNode node) {
    this.checkDefinition(node);
  }

  @Override
  protected void walkPreLoop(final AstNode node) {
    this.checkDefinition(node);
  }

  private void checkDefinition(final AstNode node) {
    final NestingLevelVisitor visitor = new NestingLevelVisitor(node);
    visitor.walkAst(node);

    final int currentNestingLevel = visitor.getNestingLevel();
    if (currentNestingLevel > this.maximumNestingLevel
        && visitor.isStartNode(node)
        && !hasParentExceedingNestingLevel(node)) {
      final String message = String.format(MESSAGE, currentNestingLevel, this.maximumNestingLevel);
      this.addIssue(node, message);
    }
  }

  private boolean hasParentExceedingNestingLevel(AstNode node) {
    AstNode parent = node.getParent();
    while (parent != null) {
      if (isNestingNode(parent)) {
        final NestingLevelVisitor visitor = new NestingLevelVisitor(parent);
        visitor.walkAst(parent);
        if (visitor.getNestingLevel() > this.maximumNestingLevel) {
          return true;
        }
      }
      parent = parent.getParent();
    }
    return false;
  }

  private boolean isNestingNode(AstNode node) {
    return node.is(MagikGrammar.IF) || node.is(MagikGrammar.LOOP);
  }
}
