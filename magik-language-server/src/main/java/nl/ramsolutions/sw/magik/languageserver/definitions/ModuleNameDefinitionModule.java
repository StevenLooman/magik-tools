package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.moduledef.api.ModuleDefinitionGrammar;

/** Provides definitions for a module name in a module.def file. */
public class ModuleNameDefinitionModule implements DefinitionModule<ModuleDefFile> {

  @Override
  public Optional<List<Location>> tryDefinitions(final DefinitionContext<ModuleDefFile> context) {
    final AstNode moduleNameNode =
        AstQuery.getParentFromChain(
            context.positionNode(),
            ModuleDefinitionGrammar.IDENTIFIER,
            ModuleDefinitionGrammar.MODULE_NAME);
    if (moduleNameNode == null) {
      return Optional.empty();
    }

    final ModuleDefFile moduleDefFile = context.file();
    final IDefinitionKeeper definitionKeeper = moduleDefFile.getDefinitionKeeper();
    final String moduleName = moduleNameNode.getTokenValue();
    final List<Location> locations =
        definitionKeeper.getModuleDefinitions(moduleName).stream()
            .map(ModuleDefinition::getLocation)
            .filter(Objects::nonNull)
            .toList();
    return Optional.of(locations);
  }
}
