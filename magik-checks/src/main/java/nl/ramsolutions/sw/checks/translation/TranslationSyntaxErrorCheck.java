package nl.ramsolutions.sw.checks.translation;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.TranslationCheck;
import nl.ramsolutions.sw.translation.api.TranslationGrammar;
import org.sonar.check.Rule;

/** Check that a translation (.trn) file has no syntax errors. */
@Rule(key = TranslationSyntaxErrorCheck.CHECK_KEY)
public class TranslationSyntaxErrorCheck extends TranslationCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "TranslationSyntaxError";

  private static final String MESSAGE = "Invalid syntax.";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getTranslationFile().getTopNode();
    topNode
        .getDescendants(TranslationGrammar.SYNTAX_ERROR)
        .forEach(node -> this.addIssue(node, MESSAGE));
  }
}
