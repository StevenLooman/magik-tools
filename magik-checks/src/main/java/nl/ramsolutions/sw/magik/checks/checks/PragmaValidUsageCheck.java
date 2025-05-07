package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/** Check if the pragma usage is valid. */
@Rule(key = PragmaValidUsageCheck.CHECK_KEY)
public class PragmaValidUsageCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "PragmaValidUsage";

  private static final String MESSAGE = "Pragma usage (%s) is invalid.";

  @Override
  protected void walkPostPragma(final AstNode node) {
    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    helper.getUsages().stream()
        .map(String::toLowerCase)
        .filter(usage -> !Pragma.USAGES.contains(usage))
        .forEach(
            usage -> {
              final String message = String.format(MESSAGE, usage);
              this.addIssue(node, message);
            });
  }
}
