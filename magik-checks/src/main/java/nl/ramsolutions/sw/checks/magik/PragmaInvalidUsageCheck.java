package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import org.sonar.check.Rule;

/** Check if the pragma usage is valid. */
@Rule(key = PragmaInvalidUsageCheck.CHECK_KEY)
public class PragmaInvalidUsageCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "PragmaInvalidUsage";

  private static final String MESSAGE = "Pragma usage (%s) is invalid.";

  @Override
  protected void walkPostPragma(final AstNode node) {
    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    helper.getPragmaParamNodes().entrySet().stream()
        .filter(entry -> entry.getKey().getTokenOriginalValue().toLowerCase().equals(Pragma.USAGE))
        .map(entry -> entry.getValue())
        .flatMap(Collection::stream)
        .filter(
            identifierNode ->
                !Pragma.USAGES.contains(identifierNode.getTokenOriginalValue().toLowerCase()))
        .forEach(
            identifierNode -> {
              final String classifyLevel = identifierNode.getTokenOriginalValue();
              final String message = String.format(MESSAGE, classifyLevel);
              this.addIssue(node, message);
            });
  }
}
