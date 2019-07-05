package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.List;

@Rule(
    key = ParserErrorCheck.CHECK_KEY,
    name = "ParserError",
    description = "Handle parser errors")
public class ParserErrorCheck extends MagikCheck {

  public static final String CHECK_KEY = "ParserError";
  private static final String MESSAGE = "Invalid code.";

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.PARSER_ERROR);
  }

  @Override
  public void visitNode(AstNode node) {
    addIssue(MESSAGE, node.getToken());
  }

}
