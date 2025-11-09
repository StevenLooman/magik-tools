package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.Set;
import nl.ramsolutions.sw.checks.DisabledByDefault;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import org.sonar.check.Rule;

/** Check if the pragma topics includes the module name. */
@DisabledByDefault
@Rule(key = PragmaDoesNotIncludeModuleNameInTopicsCheck.CHECK_KEY)
public class PragmaDoesNotIncludeModuleNameInTopicsCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "PragmaDoesNotIncludeModuleNameInTopics";

  private static final String MODULE_MESSAGE = "Module name '%s' is missing in pragma topics.";

  @Override
  protected void walkPostPragma(final AstNode node) {
    this.checkModuleName(node);
  }

  private void checkModuleName(final AstNode node) {
    final ModuleDefFile moduleDefFile = this.getMagikFile().getModuleDefFile();
    if (moduleDefFile == null) {
      return;
    }

    final String moduleName = moduleDefFile.getModuleDefinition().getName();
    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    final Set<String> topics = helper.getTopics();
    if (!topics.contains(moduleName)) {
      final String message = MODULE_MESSAGE.formatted(moduleName);
      this.addIssue(node, message);
    }
  }
}
