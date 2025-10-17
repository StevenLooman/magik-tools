package nl.ramsolutions.sw.checks.moduledef;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.ModuleDefCheck;
import nl.ramsolutions.sw.moduledef.api.ModuleDefinitionGrammar;
import org.sonar.check.Rule;

/** Check that a module.def file has no syntax errors. */
@Rule(key = ModuleDefSyntaxErrorCheck.CHECK_KEY)
public class ModuleDefSyntaxErrorCheck extends ModuleDefCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ModuleDefSyntaxError";

  private static final String MESSAGE = "Invalid syntax.";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getModuleDefFile().getTopNode();
    topNode
        .getDescendants(ModuleDefinitionGrammar.SYNTAX_ERROR)
        .forEach(node -> this.addIssue(node, MESSAGE));
  }
}
