package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/** Check if the pragma classify_level is valid. */
@Rule(key = PragmaValidClassifyLevelCheck.CHECK_KEY)
public class PragmaValidClassifyLevelCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "PragmaValidClassifyLevel";

  private static final String MESSAGE = "Pragma classify_level (%s) is invalid.";

  @Override
  protected void walkPostPragma(final AstNode node) {
    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    helper.getClassifyLevels().stream()
        .map(String::toLowerCase)
        .filter(classifyLevel -> !Pragma.CLASSIFY_LEVELS.contains(classifyLevel))
        .forEach(
            classifyLevel -> {
              final String message = String.format(MESSAGE, classifyLevel);
              this.addIssue(node, message);
            });
  }
}
