package nl.ramsolutions.sw.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.definitions.api.SwModuleDefinitionGrammar;
import nl.ramsolutions.sw.definitions.parser.SwModuleDefParser;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.ModuleDefFile;

public class ModuleDefinitionParser {

  private static final String UNDEFINED_MODULE_NAME = "_undefined_module";
  private static final String UNDEFINED_MODULE_VERSION = "_undefined_version";

  public ModuleDefinition parseDefinition(
      final ModuleDefFile moduleDefFile, final @Nullable ProductDefinition productDefinition) {
    final SwModuleDefParser parser = new SwModuleDefParser();
    final String source = moduleDefFile.getSource();
    final URI uri = moduleDefFile.getUri();
    final AstNode node = parser.parse(source, uri);

    final String moduleName;
    final String baseVersion;
    final String currentVersion;
    final AstNode moduleIdentNode =
        node.getFirstChild(SwModuleDefinitionGrammar.MODULE_IDENTIFICATION);
    if (moduleIdentNode != null) {
      final AstNode nameNode = moduleIdentNode.getFirstChild(SwModuleDefinitionGrammar.MODULE_NAME);
      moduleName = nameNode.getTokenValue();

      final List<AstNode> versionNodes =
          moduleIdentNode.getChildren(SwModuleDefinitionGrammar.VERSION);
      final AstNode baseVersionNode = versionNodes.get(0);
      baseVersion = baseVersionNode.getTokenValue();
      final AstNode currentVersionNode = versionNodes.size() > 1 ? versionNodes.get(1) : null;
      currentVersion = currentVersionNode != null ? currentVersionNode.getTokenValue() : null;
    } else {
      moduleName = ModuleDefinitionParser.UNDEFINED_MODULE_NAME;
      baseVersion = ModuleDefinitionParser.UNDEFINED_MODULE_VERSION;
      currentVersion = null;
    }

    final String productName = productDefinition != null ? productDefinition.getName() : null;

    final AstNode descriptionNode = node.getFirstChild(SwModuleDefinitionGrammar.DESCRIPTION);
    final String description =
        descriptionNode != null
            ? descriptionNode.getChildren(SwModuleDefinitionGrammar.FREE_LINES).stream()
                .map(AstNode::getTokenValue)
                .collect(Collectors.joining("\n"))
            : null;

    final AstNode requiresNode = node.getFirstChild(SwModuleDefinitionGrammar.REQUIRES);
    final List<ModuleUsage> usages =
        requiresNode != null
            ? requiresNode.getDescendants(SwModuleDefinitionGrammar.MODULE_REF).stream()
                .map(
                    moduleRefNode -> {
                      final String moduleRefName = moduleRefNode.getTokenValue();
                      final Location usageLocation = new Location(uri, moduleRefNode);
                      return new ModuleUsage(moduleRefName, usageLocation);
                    })
                .toList()
            : Collections.emptyList();

    final Location location =
        moduleIdentNode != null ? new Location(uri, moduleIdentNode) : new Location(uri);
    final Instant timestamp = moduleDefFile.getTimestamp();
    return new ModuleDefinition(
        location,
        timestamp,
        moduleName,
        productName,
        baseVersion,
        currentVersion,
        description,
        usages);
  }
}
