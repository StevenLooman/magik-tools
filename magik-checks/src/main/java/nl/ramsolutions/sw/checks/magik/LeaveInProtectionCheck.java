package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check for {@code _leave}/{@code _leave _with} inside a {@code _protection} block. */
@Rule(key = LeaveInProtectionCheck.CHECK_KEY)
public class LeaveInProtectionCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "LeaveInProtection";

  private static final String MESSAGE = "_leave is not allowed inside a _protection block.";

  @Override
  protected void walkPreLeaveStatement(final AstNode node) {
    final AstNode scopeNode =
        node.getFirstAncestor(
            MagikGrammar.LOOP,
            MagikGrammar.BLOCK,
            MagikGrammar.PROTECTION,
            MagikGrammar.METHOD_DEFINITION,
            MagikGrammar.PROCEDURE_DEFINITION);
    if (scopeNode != null && scopeNode.is(MagikGrammar.PROTECTION)) {
      this.addIssue(node, MESSAGE);
    }
  }
}
