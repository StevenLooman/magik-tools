package nl.ramsolutions.sw.checks.moduledef;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.nio.file.Path;
import nl.ramsolutions.sw.checks.ModuleDefCheck;
import nl.ramsolutions.sw.moduledef.api.ModuleDefinitionGrammar;
import org.sonar.check.Rule;

/** Check that a module definition matches the directory name. */
@Rule(key = ModuleDefNameDoesNotMatchDirectoryNameCheck.CHECK_KEY)
public class ModuleDefNameDoesNotMatchDirectoryNameCheck extends ModuleDefCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ModuleNameDoesNotMatchDirectoryName";

  private static final String MESSAGE = "Module name does not match directory name.";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getModuleDefFile().getTopNode();
    final AstNode moduleNameNode = topNode.getFirstDescendant(ModuleDefinitionGrammar.MODULE_NAME);
    if (moduleNameNode == null) {
      return;
    }

    final String moduleName = moduleNameNode.getTokenValue();
    final URI uri = this.getModuleDefFile().getUri();
    final Path path = Path.of(uri);
    final Path parentPath = path.getParent();
    if (parentPath == null) {
      return;
    }

    final String directoryName = parentPath.getFileName().toString();
    if (!moduleName.equalsIgnoreCase(directoryName)) {
      this.addIssue(moduleNameNode, MESSAGE);
    }
  }
}
