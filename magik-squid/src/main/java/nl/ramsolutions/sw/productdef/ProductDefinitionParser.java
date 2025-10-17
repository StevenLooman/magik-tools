package nl.ramsolutions.sw.productdef;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionGrammar;
import nl.ramsolutions.sw.productdef.parser.ProductDefParser;

/** {@link ProductDefinition} parser. */
public class ProductDefinitionParser {

  private static final String UNDEFINED_PRODUCT_NAME = "_undefined_product";

  public ProductDefinition parseDefinition(
      final ProductDefFile productDefFile, final @Nullable ProductDefinition parentProduct) {
    final ProductDefParser parser = new ProductDefParser();
    final String source = productDefFile.getSource();
    final URI uri = productDefFile.getUri();
    final AstNode node = parser.parse(source, uri);

    final AstNode productIdentNode =
        node.getFirstChild(ProductDefinitionGrammar.PRODUCT_IDENTIFICATION);
    final Location location =
        productIdentNode != null ? new Location(uri, productIdentNode) : new Location(uri);

    final Instant timestamp = productDefFile.getTimestamp();

    final String productName;
    if (productIdentNode != null) {
      final AstNode nameNode =
          productIdentNode.getFirstChild(ProductDefinitionGrammar.PRODUCT_NAME);
      productName = nameNode.getTokenValue();
    } else {
      productName = ProductDefinitionParser.UNDEFINED_PRODUCT_NAME;
    }

    final String parentProductName = parentProduct != null ? parentProduct.getName() : null;

    final AstNode versionNode = node.getFirstChild(ProductDefinitionGrammar.VERSION);
    final String version =
        versionNode != null
            ? versionNode.getFirstChild(ProductDefinitionGrammar.VERSION_NUMBER).getTokenValue()
            : null;
    final AstNode versionCommentNode =
        versionNode != null
            ? versionNode.getFirstChild(ProductDefinitionGrammar.REST_OF_LINE)
            : null;
    final String versionComment =
        versionCommentNode != null ? versionCommentNode.getTokenValue() : null;

    final AstNode titleNode = node.getFirstChild(ProductDefinitionGrammar.TITLE);
    final String title =
        titleNode != null
            ? titleNode.getChildren(ProductDefinitionGrammar.FREE_LINES).stream()
                .map(AstNode::getTokenValue)
                .collect(Collectors.joining("\n"))
            : null;

    final AstNode descriptionNode = node.getFirstChild(ProductDefinitionGrammar.DESCRIPTION);
    final String description =
        descriptionNode != null
            ? descriptionNode.getChildren(ProductDefinitionGrammar.FREE_LINES).stream()
                .map(AstNode::getTokenValue)
                .collect(Collectors.joining("\n"))
            : null;

    final AstNode requiresNode = node.getFirstChild(ProductDefinitionGrammar.REQUIRES);
    final List<ProductUsage> usages =
        requiresNode != null
            ? requiresNode.getDescendants(ProductDefinitionGrammar.PRODUCT_REF).stream()
                .map(
                    productRefNode -> {
                      final String productRefName = productRefNode.getTokenValue();
                      final Location usageLocation = new Location(uri, productRefNode);
                      return new ProductUsage(productRefName, usageLocation);
                    })
                .toList()
            : Collections.emptyList();

    return new ProductDefinition(
        location,
        timestamp,
        productName,
        parentProductName,
        version,
        versionComment,
        title,
        description,
        usages);
  }
}
