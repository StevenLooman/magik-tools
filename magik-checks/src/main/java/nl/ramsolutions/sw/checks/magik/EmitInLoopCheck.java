package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check for emit statements inside a loop body. */
@Rule(key = EmitInLoopCheck.CHECK_KEY)
public class EmitInLoopCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "EmitInLoop";

  private static final String MESSAGE = "Emit statement inside loop body has no effect.";

  @Override
  protected void walkPreEmitStatement(final AstNode node) {
    final AstNode bodyNode = node.getFirstAncestor(MagikGrammar.BODY);
    if (bodyNode != null && bodyNode.getParent().is(MagikGrammar.LOOP)) {
      this.addIssue(node, MESSAGE);
    }
  }
}
