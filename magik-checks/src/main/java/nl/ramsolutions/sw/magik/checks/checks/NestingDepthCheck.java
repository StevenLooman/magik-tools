package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check nesting depth of nodes. */
@Rule(key = NestingDepthCheck.CHECK_KEY)
public class NestingDepthCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "NestingDepth";

  private static final int DEFAULT_MAXIMUM_NESTING_DEPTH = 3;
  private static final String MESSAGE = "The nesting depth is greater than permitted (%s).";

  /** Maximum nesting depth of node. */
  @RuleProperty(
      key = "maximum nesting depth",
      defaultValue = "" + DEFAULT_MAXIMUM_NESTING_DEPTH,
      description = "Maximum nesting depth",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int maximumNestingDepth = DEFAULT_MAXIMUM_NESTING_DEPTH;

  private int currentNestingDepth = 0;

  @Override
  protected void walkPreBody(final AstNode node) {
    if (node != null && node.getToken() != null) {
      this.currentNestingDepth++;
    }

    super.walkPreBody(node);
  }

  @Override
  protected void walkPostBody(final AstNode node) {
    if (node != null && node.getToken() != null) {
      this.checkNestingDepth(node);
      this.currentNestingDepth--;
    }

    super.walkPostBody(node);
  }

  private int getCurrentDepth() {
    return this.currentNestingDepth - 1;
  }

  private void checkNestingDepth(final AstNode node) {
    if (getCurrentDepth() == this.maximumNestingDepth) {
      this.addIssue(node, String.format(MESSAGE, this.maximumNestingDepth));
    }
  }
}
