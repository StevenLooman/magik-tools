package nl.ramsolutions.sw.checks.moduledef;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.ModuleDefCheck;
import nl.ramsolutions.sw.moduledef.api.ModuleDefinitionGrammar;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check that a module definition matches the directory name. */
@Rule(key = ModuleDefNameDoesNotMatchDirectoryNameCheck.CHECK_KEY)
public class ModuleDefNameDoesNotMatchDirectoryNameCheck extends ModuleDefCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ModuleDefNameDoesNotMatchDirectoryName";

  private static final String MESSAGE = "Module name does not match directory name.";

  private static final String DEFAULT_IGNORED_DIRECTORY_NAMES = "magik_sessions";

  @RuleProperty(
      key = "ignored directory names",
      defaultValue = "" + DEFAULT_IGNORED_DIRECTORY_NAMES,
      description = "List of ignored directory names, separated by ','",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String ignoredDirectoryNames = DEFAULT_IGNORED_DIRECTORY_NAMES;

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
    final String loweredDirectoryName = directoryName.toLowerCase();
    final Set<String> ignoredDirectoryNames = this.getIgnoredDirectoryNames();
    if (ignoredDirectoryNames.contains(loweredDirectoryName)) {
      return;
    }

    if (!moduleName.toLowerCase().equalsIgnoreCase(loweredDirectoryName)) {
      this.addIssue(moduleNameNode, MESSAGE);
    }
  }

  private Set<String> getIgnoredDirectoryNames() {
    return Arrays.stream(this.ignoredDirectoryNames.split(","))
        .map(String::trim)
        .map(String::toLowerCase)
        .collect(Collectors.toSet());
  }
}
