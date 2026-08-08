package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Provides hover for a package identifier, including its uses-hierarchy. */
public class PackageHoverModule implements HoverModule<MagikTypedFile> {

  @Override
  public Optional<String> tryHover(final HoverContext<MagikTypedFile> context) {
    final AstNode hoveredNode = context.getHoveredNode();
    if (!hoveredNode.is(MagikGrammar.PACKAGE_IDENTIFIER)) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.file();
    final String packageName = hoveredNode.getTokenValue();
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final StringBuilder builder = new StringBuilder();
    definitionKeeper
        .getPackageDefinitions(packageName)
        .forEach(
            pakkageDef -> {
              // Name.
              builder.append("## ").append(pakkageDef.getName()).append(HoverUtils.SECTION_END);

              // Doc.
              final String doc = pakkageDef.getDoc();
              if (doc != null) {
                builder.append(HoverUtils.docToMarkdown(doc)).append(HoverUtils.SECTION_END);
              }

              // Uses.
              this.addPackageHierarchy(magikFile, pakkageDef, builder, 0);
            });
    return Optional.of(builder.toString());
  }

  private void addPackageHierarchy(
      final MagikTypedFile magikFile,
      final PackageDefinition pakkageDef,
      final StringBuilder builder,
      final int indent) {
    if (indent == 0) {
      builder.append(pakkageDef.getName()).append("\n\n");
    }

    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final String indentStr = HoverUtils.NBSP_NBSP.repeat(indent);
    pakkageDef.getUses().stream()
        .sorted()
        .forEach(
            use -> {
              builder.append(indentStr).append(" ↳ ").append(use).append("\n\n");

              definitionKeeper
                  .getPackageDefinitions(use)
                  .forEach(
                      usePakkageDef ->
                          this.addPackageHierarchy(magikFile, usePakkageDef, builder, indent + 1));
            });

    if (indent == 0) {
      builder.append(HoverUtils.SECTION_END);
    }
  }
}
