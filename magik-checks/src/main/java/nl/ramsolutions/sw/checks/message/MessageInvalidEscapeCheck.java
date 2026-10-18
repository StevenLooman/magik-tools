package nl.ramsolutions.sw.checks.message;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MessageCheck;
import nl.ramsolutions.sw.message.api.MessageGrammar;
import org.sonar.check.Rule;

/** Check that a {@code #} in the text of a message is a parameter or a special character. */
@Rule(key = MessageInvalidEscapeCheck.CHECK_KEY)
public class MessageInvalidEscapeCheck extends MessageCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MessageInvalidEscape";

  private static final String MESSAGE =
      "'%s' is not a parameter or a special character, it is left out of the text.";
  private static final String MESSAGE_PARAMETER_ZERO =
      "Parameter '#0' does not exist, parameters start at '#1'.";
  private static final String PARAMETER_ZERO = "#0";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getMessageFile().getTopNode();
    topNode.getDescendants(MessageGrammar.INVALID_ESCAPE).forEach(this::addIssue);
  }

  private void addIssue(final AstNode node) {
    final String value = node.getTokenValue();
    final String message =
        value.equals(PARAMETER_ZERO) ? MESSAGE_PARAMETER_ZERO : MESSAGE.formatted(value);
    this.addIssue(node, message);
  }
}
