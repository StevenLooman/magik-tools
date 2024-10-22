package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayDeque;
import java.util.Deque;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check nesting level of nodes. */
@Rule(key = NestingLevelCheck.CHECK_KEY)
public class NestingLevelCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "NestingLevel";

  private static final int DEFAULT_MAXIMUM_NESTING_LEVEL = 3;
  private static final String MESSAGE = "Nesting level greater than permitted (%s).";

  /** Maximum nesting level of node. */
  @RuleProperty(
      key = "maximum nesting level",
      defaultValue = "" + DEFAULT_MAXIMUM_NESTING_LEVEL,
      description = "Maximum nesting level",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int maximumNestingLevel = DEFAULT_MAXIMUM_NESTING_LEVEL;

  private Deque<AstNode> depthNodes = new ArrayDeque<>();

  @Override
  protected void walkPreIf(final AstNode node) {
    this.checkDefinition(node);
  }

  @Override
  protected void walkPostIf(final AstNode node) {
    depthNodes.pop();
  }

  @Override
  protected void walkPreLoop(final AstNode node) {
    this.checkDefinition(node);
  }

  @Override
  protected void walkPostLoop(final AstNode node) {
    depthNodes.pop();
  }

  @Override
  protected void walkPreTry(final AstNode node) {
    this.checkDefinition(node);
  }

  @Override
  protected void walkPostTry(final AstNode node) {
    depthNodes.pop();
  }

  private void checkDefinition(final AstNode node) {
    depthNodes.push(node);

    if (depthNodes.size() == DEFAULT_MAXIMUM_NESTING_LEVEL + 1) {
      AstNode lastNode = depthNodes.peek();
      this.addIssue(lastNode, String.format(MESSAGE, DEFAULT_MAXIMUM_NESTING_LEVEL));
    }
  }
}
