package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.Set;
import nl.ramsolutions.sw.checks.DisabledByDefault;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import org.sonar.check.Rule;

/** Check if the pragma topics include the module name. */
@DisabledByDefault
@Rule(key = PragmaTopicsDoesNotIncludeModuleNameCheck.CHECK_KEY)
public class PragmaTopicsDoesNotIncludeModuleNameCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "PragmaTopicsDoesNotIncludeModuleName";

  private static final String MESSAGE = "Module name '%s' is missing in pragma topics.";

  @Override
  protected void walkPostPragma(final AstNode node) {
    final URI uri = node.getToken().getURI();
    final String moduleName = ModuleDefFile.getModuleNameForUri(uri);

    if (moduleName == null) {
      return;
    }

    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    final Set<String> topics = helper.getTopics();

    if (!topics.contains(moduleName)) {
      final String message = MESSAGE.formatted(moduleName);
      this.addIssue(node, message);
    }
  }
}
