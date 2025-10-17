package nl.ramsolutions.sw.checks.moduledef;

import com.sonar.sslr.api.AstNode;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.ModuleDefCheck;
import nl.ramsolutions.sw.moduledef.api.ModuleDefinitionGrammar;
import org.sonar.check.Rule;

/** Check that a module.def file has a description. */
@Rule(key = ModuleDefMissingDescriptionCheck.CHECK_KEY)
public class ModuleDefMissingDescriptionCheck extends ModuleDefCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ModuleDefMissingDescription";

  private static final String MESSAGE = "Module description is missing, or is empty.";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getModuleDefFile().getTopNode();
    final AstNode descriptionNode = topNode.getFirstDescendant(ModuleDefinitionGrammar.DESCRIPTION);
    if (descriptionNode == null) {
      this.addFileIssue(MESSAGE);
      return;
    }

    final AstNode descriptionNodeLines =
        descriptionNode.getFirstChild(ModuleDefinitionGrammar.FREE_LINES);
    final String description =
        descriptionNodeLines.getChildren(ModuleDefinitionGrammar.FREE_LINE).stream()
            .map(AstNode::getTokenValue)
            .map(String::trim)
            .collect(Collectors.joining());
    if (description.isEmpty()) {
      this.addIssue(descriptionNode, MESSAGE);
    }
  }
}
