package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.List;

@Rule(
    key = SyntaxErrorCheck.CHECK_KEY,
    name = "SyntaxError",
    description = "Handle parser errors")
public class SyntaxErrorCheck extends MagikCheck {

  public static final String CHECK_KEY = "SyntaxError";
  private static final String MESSAGE = "Invalid code.";

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.SYNTAX_ERROR);
  }

  @Override
  public void visitNode(AstNode node) {
    addIssue(MESSAGE, node.getToken());
  }

}
