package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/** Check for syntax errors. */
@Rule(key = SyntaxErrorCheck.CHECK_KEY, name = "SyntaxError", description = "Handle parser errors")
public class SyntaxErrorCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "SyntaxError";

  private static final String MESSAGE = "Invalid code.";

  @Override
  protected void walkPreSyntaxError(final AstNode node) {
    node.getTokens().forEach(token -> this.addIssue(token, MESSAGE));
  }
}
