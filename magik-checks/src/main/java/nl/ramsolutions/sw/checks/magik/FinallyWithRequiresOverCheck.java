package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check for a {@code _finally _with} clause on a loop without an {@code _over} parent. */
@Rule(key = FinallyWithRequiresOverCheck.CHECK_KEY)
public class FinallyWithRequiresOverCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "FinallyWithRequiresOver";

  private static final String MESSAGE = "_finally _with is only supported with an _over loop.";

  @Override
  protected void walkPreFinally(final AstNode node) {
    if (node.getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER) == null) {
      return;
    }

    final AstNode loopNode = node.getParent();
    if (!loopNode.getParent().is(MagikGrammar.OVER)) {
      this.addIssue(node, MESSAGE);
    }
  }
}
