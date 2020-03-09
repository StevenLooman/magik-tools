package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;

@Rule(
    key = SyntaxErrorCheck.CHECK_KEY,
    name = "SyntaxError",
    description = "Handle parser errors")
public class SyntaxErrorCheck extends MagikCheck {

  public static final String CHECK_KEY = "SyntaxError";
  private static final String MESSAGE = "Invalid code.";

  @Override
  protected void walkPreSyntaxError(AstNode node) {
    addIssue(MESSAGE, node.getToken());
  }

}
