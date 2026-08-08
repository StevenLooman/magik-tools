package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Provides hover for a condition name, including its taxonomy and data names. */
public class ConditionHoverModule implements HoverModule<MagikTypedFile> {

  @Override
  public Optional<String> tryHover(final HoverContext<MagikTypedFile> context) {
    final AstNode hoveredNode = context.getHoveredNode();
    final AstNode parentNode = hoveredNode.getParent();
    if (parentNode == null || !parentNode.is(MagikGrammar.CONDITION_NAME)) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.file();
    final String conditionName = hoveredNode.getTokenValue();
    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final StringBuilder builder = new StringBuilder();
    definitionKeeper
        .getConditionDefinitions(conditionName)
        .forEach(
            conditionDef -> {
              // Name.
              builder.append("## ").append(conditionDef.getName()).append(HoverUtils.SECTION_END);

              // Doc.
              final String doc = conditionDef.getDoc();
              if (doc != null) {
                builder.append(HoverUtils.docToMarkdown(doc)).append(HoverUtils.SECTION_END);
              }

              // Taxonomy.
              builder.append("## Taxonomy: \n\n");
              this.addConditionTaxonomy(magikFile, conditionDef, builder, 0);
              builder.append(HoverUtils.SECTION_END);

              // Data names.
              builder.append("## Data:\n");
              conditionDef
                  .getDataNames()
                  .forEach(dataName -> builder.append("* ").append(dataName).append("\n"));
            });
    return Optional.of(builder.toString());
  }

  private void addConditionTaxonomy(
      final MagikTypedFile magikFile,
      final ConditionDefinition conditionDef,
      final StringBuilder builder,
      final int indent) {
    if (indent == 0) {
      builder.append(conditionDef.getName()).append("\n\n");
    }

    final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
    final String indentStr = HoverUtils.NBSP_NBSP.repeat(indent);
    final String parentConditionName = conditionDef.getParent();
    if (parentConditionName != null) {
      builder.append(indentStr).append(" ↳ ").append(parentConditionName).append("\n\n");

      definitionKeeper
          .getConditionDefinitions(parentConditionName)
          .forEach(
              parentConditionDef ->
                  this.addConditionTaxonomy(magikFile, parentConditionDef, builder, indent + 1));
    }

    if (indent == 0) {
      builder.append(HoverUtils.SECTION_END);
    }
  }
}
