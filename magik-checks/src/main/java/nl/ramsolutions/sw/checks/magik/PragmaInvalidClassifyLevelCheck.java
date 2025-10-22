package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import org.sonar.check.Rule;

/** Check if the pragma classify_level is valid. */
@Rule(key = PragmaInvalidClassifyLevelCheck.CHECK_KEY)
public class PragmaInvalidClassifyLevelCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "PragmaInvalidClassifyLevel";

  private static final String MESSAGE = "Pragma classify_level (%s) is invalid.";

  @Override
  protected void walkPostPragma(final AstNode node) {
    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    helper.getPragmaParamNodes().entrySet().stream()
        .filter(
            entry ->
                entry.getKey().getTokenOriginalValue().toLowerCase().equals(Pragma.CLASSIFY_LEVEL))
        .map(entry -> entry.getValue())
        .flatMap(Collection::stream)
        .filter(
            identifierNode ->
                !Pragma.CLASSIFY_LEVELS.contains(
                    identifierNode.getTokenOriginalValue().toLowerCase()))
        .forEach(
            identifierNode -> {
              final String classifyLevel = identifierNode.getTokenOriginalValue();
              final String message = MESSAGE.formatted(classifyLevel);
              this.addIssue(node, message);
            });
  }
}
