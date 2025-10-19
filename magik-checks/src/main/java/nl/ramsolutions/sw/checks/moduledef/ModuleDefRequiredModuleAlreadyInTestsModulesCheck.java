package nl.ramsolutions.sw.checks.moduledef;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.ModuleDefCheck;
import nl.ramsolutions.sw.moduledef.api.ModuleDefinitionGrammar;
import org.sonar.check.Rule;

/** Check that no module listed in requires also appears in tests_modules. */
@Rule(key = ModuleDefRequiredModuleAlreadyInTestsModulesCheck.CHECK_KEY)
public class ModuleDefRequiredModuleAlreadyInTestsModulesCheck extends ModuleDefCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ModuleDefRequiredModuleAlreadyInTestsModules";

  private static final String MESSAGE =
      "Module '%s' already in tests_modules, no need to list in requires.";

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

    final List<AstNode> requiresModules =
        requiresNode.getDescendants(ModuleDefinitionGrammar.MODULE_NAME);
    final Set<String> testsModules =
        testsModulesNode.getDescendants(ModuleDefinitionGrammar.MODULE_NAME).stream()
            .map(AstNode::getTokenValue)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());

    for (final AstNode moduleNode : requiresModules) {
      final String moduleName = moduleNode.getTokenValue();
      if (testsModules.stream().anyMatch(moduleName::equalsIgnoreCase)) {
        this.addIssue(moduleNode, String.format(MESSAGE, moduleName));
      }
    }
  }
}
