package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionGrammar;

/** Provides definitions for a product name in a product.def file. */
public class ProductNameDefinitionModule implements DefinitionModule<ProductDefFile> {

  @Override
  public Optional<List<Location>> tryDefinitions(final DefinitionContext<ProductDefFile> context) {
    final AstNode productNameNode =
        AstQuery.getParentFromChain(
            context.positionNode(),
            ProductDefinitionGrammar.IDENTIFIER,
            ProductDefinitionGrammar.PRODUCT_NAME);
    if (productNameNode == null) {
      return Optional.empty();
    }

    final ProductDefFile productDefFile = context.file();
    final IDefinitionKeeper definitionKeeper = productDefFile.getDefinitionKeeper();
    final String productName = productNameNode.getTokenValue();
    final List<Location> locations =
        definitionKeeper.getProductDefinitions(productName).stream()
            .map(ProductDefinition::getLocation)
            .toList();
    return Optional.of(locations);
  }
}
