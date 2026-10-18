package nl.ramsolutions.sw.checks.translation;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.TranslationCheck;
import nl.ramsolutions.sw.translation.api.TranslationGrammar;
import org.sonar.check.Rule;

/** Check that a translation has no fields after the translated text, as these are ignored. */
@Rule(key = TranslationIgnoredFieldCheck.CHECK_KEY)
public class TranslationIgnoredFieldCheck extends TranslationCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "TranslationIgnoredField";

  private static final String MESSAGE = "Field after the translated text is ignored.";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getTranslationFile().getTopNode();
    topNode.getChildren(TranslationGrammar.TRANSLATION).stream()
        .map(node -> node.getFirstChild(TranslationGrammar.IGNORED_FIELD))
        .filter(node -> node != null)
        .forEach(node -> this.addIssue(node, MESSAGE));
  }
}
