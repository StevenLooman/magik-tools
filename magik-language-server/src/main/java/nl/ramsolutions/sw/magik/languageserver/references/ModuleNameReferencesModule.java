package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleUsage;
import nl.ramsolutions.sw.moduledef.api.ModuleDefinitionGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides references to a module name in a module.def file. */
public class ModuleNameReferencesModule implements ReferencesModule<ModuleDefFile> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModuleNameReferencesModule.class);

  @Override
  public Optional<List<Location>> tryReferences(final ReferencesContext<ModuleDefFile> context) {
    final AstNode moduleNameNode =
        AstQuery.getParentFromChain(
            context.positionNode(),
            ModuleDefinitionGrammar.IDENTIFIER,
            ModuleDefinitionGrammar.MODULE_NAME);
    if (moduleNameNode == null) {
      return Optional.empty();
    }

    final String moduleName = moduleNameNode.getTokenValue();
    LOGGER.debug("Finding references to product: {}", moduleName);

    final ModuleDefFile moduleDefFile = context.file();
    final IDefinitionKeeper definitionKeeper = moduleDefFile.getDefinitionKeeper();
    final ModuleUsage searchedModuleUsage = new ModuleUsage(moduleName, null);
    final List<Location> locations =
        definitionKeeper.getModuleDefinitions().stream()
            .flatMap(
                def ->
                    Stream.concat(def.getRequiredModules().stream(), def.getTestModules().stream()))
            .filter(moduleUsage -> moduleUsage.equals(searchedModuleUsage))
            .map(ModuleUsage::getLocation)
            .toList();
    return Optional.of(locations);
  }
}
