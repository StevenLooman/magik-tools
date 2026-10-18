package nl.ramsolutions.sw.checks.message;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MessageCheck;
import nl.ramsolutions.sw.message.api.MessageGrammar;
import org.sonar.check.Rule;

/** Check that a message (.msg) file has no syntax errors. */
@Rule(key = MessageSyntaxErrorCheck.CHECK_KEY)
public class MessageSyntaxErrorCheck extends MessageCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MessageSyntaxError";

  private static final String MESSAGE = "Invalid syntax.";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getMessageFile().getTopNode();
    topNode
        .getDescendants(MessageGrammar.SYNTAX_ERROR)
        .forEach(node -> this.addIssue(node, MESSAGE));
  }
}
