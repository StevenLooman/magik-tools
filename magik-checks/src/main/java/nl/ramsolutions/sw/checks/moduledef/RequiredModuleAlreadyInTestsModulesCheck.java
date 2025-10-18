package nl.ramsolutions.sw.checks.moduledef;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.checks.ModuleDefCheck;
import nl.ramsolutions.sw.moduledef.api.ModuleDefinitionGrammar;
import org.sonar.check.Rule;

/** Check that no module listed in REQUIRES also appears in TESTS_MODULES. */
@Rule(key = RequiredModuleAlreadyInTestsModulesCheck.CHECK_KEY)
public class RequiredModuleAlreadyInTestsModulesCheck extends ModuleDefCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "RequiredModuleAlreadyInTestsModules";

  private static final String MESSAGE = "Module '%s' is listed in both requires and tests_modules.";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getModuleDefFile().getTopNode();
    if (topNode == null) {
      return;
    }

    final AstNode requiresNode = topNode.getFirstDescendant(ModuleDefinitionGrammar.REQUIRES);
    final AstNode testsModulesNode =
        topNode.getFirstDescendant(ModuleDefinitionGrammar.TESTS_MODULES);

    if (requiresNode == null || testsModulesNode == null) {
      return;
    }

    final List<AstNode> requiresModules = this.getModuleNameNodes(requiresNode);
    final Set<String> testsModules = this.getModuleNames(testsModulesNode);

    for (AstNode moduleNode : requiresModules) {
      final String moduleName = moduleNode.getTokenValue().trim();
      if (testsModules.contains(moduleName)) {
        this.addIssue(moduleNode, String.format(MESSAGE, moduleName));
      }
    }
  }
}
