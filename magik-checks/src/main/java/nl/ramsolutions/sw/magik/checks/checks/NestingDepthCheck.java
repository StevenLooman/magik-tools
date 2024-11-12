package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check nesting depth of nodes. */
@Rule(key = NestingDepthCheck.CHECK_KEY)
public class NestingDepthCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "NestingDepth";

  private static final int DEFAULT_MAX_NESTING_DEPTH = 3;
  private static final boolean DEFAULT_COUNT_EARLY_RETURN_AS_NESTING_DEPTH = true;
  private static final String MESSAGE = "The nesting depth is greater than permitted (%s).";

  /** Maximum nesting depth of node. */
  @RuleProperty(
      key = "max nesting depth",
      defaultValue = "" + DEFAULT_MAX_NESTING_DEPTH,
      description = "Maximum nesting depth",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int maxNestingDepth = DEFAULT_MAX_NESTING_DEPTH;

  /** Count early return as nesting depth. */
  @RuleProperty(
      key = "count early return as nesting depth",
      defaultValue = "" + DEFAULT_COUNT_EARLY_RETURN_AS_NESTING_DEPTH,
      description = "Count early return as nesting depth",
      type = "BOOLEAN")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public boolean countEarlyReturnAsNestingDepth = DEFAULT_COUNT_EARLY_RETURN_AS_NESTING_DEPTH;

  private int currentNestingDepth = 0;

  @Override
  protected void walkPreBody(final AstNode node) {
    if (!countEarlyReturnAsNestingDepth && isEarlyReturn(node)) {
      return;
    }

    this.currentNestingDepth++;
  }

  @Override
  protected void walkPostBody(final AstNode node) {
    if (node.hasChildren()) {
      // Skip empty bodies, addIssue() cannot handle nodes without tokens.
      this.checkNestingDepth(node);
    }

    this.currentNestingDepth = Math.max(0, this.currentNestingDepth - 1);
  }

  private int getCurrentDepth() {
    // Subtract one to ignore the first body. This is most likely the method body itself.
    return this.currentNestingDepth - 1;
  }

  private void checkNestingDepth(final AstNode node) {
    if (this.getCurrentDepth() == this.maxNestingDepth) {
      final String message = String.format(MESSAGE, this.maxNestingDepth);
      this.addIssue(node, message);
    }
  }

  private boolean isEarlyReturn(final AstNode node) {
    if (!node.hasChildren()) {
      return false;
    }

    AstNode statement = node.getFirstChild();
    return statement.getFirstChild(
            MagikGrammar.LEAVE_STATEMENT,
            MagikGrammar.CONTINUE_STATEMENT,
            MagikGrammar.RETURN_STATEMENT)
        != null;
  }
}
