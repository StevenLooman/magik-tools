package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayDeque;
import java.util.Deque;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check nesting depth of nodes. */
@Rule(key = NestingDepthCheck.CHECK_KEY)
public class NestingDepthCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "NestingDepth";

  private static final int DEFAULT_MAXIMUM_NESTING_DEPTH = 3;
  private static final String MESSAGE = "Nesting Depth greater than permitted (%s).";

  /** Maximum nesting depth of node. */
  @RuleProperty(
      key = "maximum nesting depth",
      defaultValue = "" + DEFAULT_MAXIMUM_NESTING_DEPTH,
      description = "Maximum nesting depth",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int maximumNestingDepth = DEFAULT_MAXIMUM_NESTING_DEPTH;

  private Deque<AstNode> depthNodes = new ArrayDeque<>();

  @Override
  protected void walkPreIf(final AstNode node) {
    this.checkNestingDepth(node);
  }

  @Override
  protected void walkPostIf(final AstNode node) {
    depthNodes.pop();
  }

  @Override
  protected void walkPreLoop(final AstNode node) {
    this.checkNestingDepth(node);
  }

  @Override
  protected void walkPostLoop(final AstNode node) {
    depthNodes.pop();
  }

  @Override
  protected void walkPreTry(final AstNode node) {
    this.checkNestingDepth(node);
  }

  @Override
  protected void walkPostTry(final AstNode node) {
    depthNodes.pop();
  }

  private void checkNestingDepth(final AstNode node) {
    depthNodes.push(node);

    if (depthNodes.size() == DEFAULT_MAXIMUM_NESTING_DEPTH + 1) {
      AstNode lastNode = depthNodes.peek();
      this.addIssue(lastNode, String.format(MESSAGE, DEFAULT_MAXIMUM_NESTING_DEPTH));
    }
  }
}
