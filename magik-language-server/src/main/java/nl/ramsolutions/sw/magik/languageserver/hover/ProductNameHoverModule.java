package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.Objects;
import java.util.Optional;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionGrammar;

/** Provides hover for a product name in a product.def file. */
public class ProductNameHoverModule implements HoverModule<ProductDefFile> {

  @Override
  public Optional<String> tryHover(final HoverContext<ProductDefFile> context) {
    final AstNode productNameNode =
        AstQuery.getParentFromChain(
            context.hoveredTokenNode(),
            ProductDefinitionGrammar.IDENTIFIER,
            ProductDefinitionGrammar.PRODUCT_NAME);
    if (productNameNode == null) {
      return Optional.empty();
    }

    final ProductDefFile productDefFile = context.file();
    final IDefinitionKeeper definitionKeeper = productDefFile.getDefinitionKeeper();
    final String productName = productNameNode.getTokenValue().toLowerCase();
    final StringBuilder builder = new StringBuilder();
    definitionKeeper
        .getProductDefinitions(productName)
        .forEach(productDef -> this.buildProductDefDoc(productDef, builder));
    return Optional.of(builder.toString());
  }

  private void buildProductDefDoc(final ProductDefinition productDef, final StringBuilder builder) {
    final String productName = productDef.getName();
    builder.append("## ").append(productName);

    final String title = productDef.getTitle();
    if (title != null) {
      builder.append("\n").append(HoverUtils.docToMarkdown(title));
    }
    builder.append(HoverUtils.SECTION_END);

    final String version = Objects.requireNonNullElse(productDef.getVersion(), "");
    final String versionComment = Objects.requireNonNullElse(productDef.getVersionComment(), "");
    builder
        .append("Version: ")
        .append(version)
        .append(" ")
        .append(versionComment)
        .append(HoverUtils.SECTION_END);

    final String description = productDef.getDescription();
    if (description != null) {
      builder.append("## Description").append("\n");
      builder.append(HoverUtils.docToMarkdown(description)).append(HoverUtils.SECTION_END);
    }
  }
}
