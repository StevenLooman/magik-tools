package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.ProductUsage;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides references to a product name in a product.def file. */
public class ProductNameReferencesModule implements ReferencesModule<ProductDefFile> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductNameReferencesModule.class);

  @Override
  public Optional<List<Location>> tryReferences(final ReferencesContext<ProductDefFile> context) {
    final AstNode productNameNode =
        AstQuery.getParentFromChain(
            context.positionNode(),
            ProductDefinitionGrammar.IDENTIFIER,
            ProductDefinitionGrammar.PRODUCT_NAME);
    if (productNameNode == null) {
      return Optional.empty();
    }

    final String productName = productNameNode.getTokenValue();
    LOGGER.debug("Finding references to product: {}", productName);

    final ProductDefFile productDefFile = context.file();
    final IDefinitionKeeper definitionKeeper = productDefFile.getDefinitionKeeper();
    final ProductUsage searchedProductUsage = new ProductUsage(productName, null);
    final List<Location> locations =
        definitionKeeper.getProductDefinitions().stream()
            .flatMap(def -> def.getUsages().stream())
            .filter(productUsage -> productUsage.equals(searchedProductUsage))
            .map(ProductUsage::getLocation)
            .toList();
    return Optional.of(locations);
  }
}
