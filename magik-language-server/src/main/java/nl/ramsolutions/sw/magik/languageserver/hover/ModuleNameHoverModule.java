package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.Objects;
import java.util.Optional;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.moduledef.api.ModuleDefinitionGrammar;

/** Provides hover for a module name in a module.def file. */
public class ModuleNameHoverModule implements HoverModule<ModuleDefFile> {

  @Override
  public Optional<String> tryHover(final HoverContext<ModuleDefFile> context) {
    final AstNode moduleNameNode =
        AstQuery.getParentFromChain(
            context.hoveredTokenNode(),
            ModuleDefinitionGrammar.IDENTIFIER,
            ModuleDefinitionGrammar.MODULE_NAME);
    if (moduleNameNode == null) {
      return Optional.empty();
    }

    final ModuleDefFile moduleDefFile = context.file();
    final IDefinitionKeeper definitionKeeper = moduleDefFile.getDefinitionKeeper();
    final String moduleName = moduleNameNode.getTokenValue().toLowerCase();
    final StringBuilder builder = new StringBuilder();
    definitionKeeper
        .getModuleDefinitions(moduleName)
        .forEach(moduleDef -> this.buildModuleDefDoc(moduleDef, builder));
    return Optional.of(builder.toString());
  }

  private void buildModuleDefDoc(final ModuleDefinition moduleDef, final StringBuilder builder) {
    final String moduleName = moduleDef.getName();
    builder.append("## ").append(moduleName).append(HoverUtils.SECTION_END);

    final String baseVersion = moduleDef.getBaseVersion();
    final String currentVersion = Objects.requireNonNullElse(moduleDef.getCurrentVersion(), "");
    builder
        .append("Version: ")
        .append(baseVersion)
        .append(" ")
        .append(currentVersion)
        .append(HoverUtils.SECTION_END);

    final String description = moduleDef.getDescription();
    if (description != null) {
      builder.append("## Description").append("\n");
      builder.append(HoverUtils.docToMarkdown(description)).append(HoverUtils.SECTION_END);
    }
  }
}
