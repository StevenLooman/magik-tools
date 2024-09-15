package nl.ramsolutions.sw.productdef;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.productdef.api.SwProductDefinitionGrammar;
import nl.ramsolutions.sw.productdef.parser.SwProductDefParser;

/** {@link ProductDefinition} parser. */
public class ProductDefinitionParser {

  private static final String UNDEFINED_PRODUCT_NAME = "_undefined_product";

  public ProductDefinition parseDefinition(
      final ProductDefFile productDefFile, final @Nullable ProductDefinition parentProduct) {
    final SwProductDefParser parser = new SwProductDefParser();
    final String source = productDefFile.getSource();
    final URI uri = productDefFile.getUri();
    final AstNode node = parser.parse(source, uri);

    final AstNode productIdentNode =
        node.getFirstChild(SwProductDefinitionGrammar.PRODUCT_IDENTIFICATION);
    final Location location =
        productIdentNode != null ? new Location(uri, productIdentNode) : new Location(uri);

    final Instant timestamp = productDefFile.getTimestamp();

    final String productName;
    if (productIdentNode != null) {
      final AstNode nameNode =
          productIdentNode.getFirstChild(SwProductDefinitionGrammar.PRODUCT_NAME);
      productName = nameNode.getTokenValue();
    } else {
      productName = ProductDefinitionParser.UNDEFINED_PRODUCT_NAME;
    }

    final String parentProductName = parentProduct != null ? parentProduct.getName() : null;

    final AstNode versionNode = node.getFirstChild(SwProductDefinitionGrammar.VERSION);
    final String version =
        versionNode != null
            ? versionNode.getFirstChild(SwProductDefinitionGrammar.VERSION_NUMBER).getTokenValue()
            : null;
    final AstNode versionCommentNode =
        versionNode != null
            ? versionNode.getFirstChild(SwProductDefinitionGrammar.REST_OF_LINE)
            : null;
    final String versionComment =
        versionCommentNode != null ? versionCommentNode.getTokenValue() : null;

    final AstNode titleNode = node.getFirstChild(SwProductDefinitionGrammar.TITLE);
    final String title =
        titleNode != null
            ? titleNode.getChildren(SwProductDefinitionGrammar.FREE_LINES).stream()
                .map(AstNode::getTokenValue)
                .collect(Collectors.joining("\n"))
            : null;

    final AstNode descriptionNode = node.getFirstChild(SwProductDefinitionGrammar.DESCRIPTION);
    final String description =
        descriptionNode != null
            ? descriptionNode.getChildren(SwProductDefinitionGrammar.FREE_LINES).stream()
                .map(AstNode::getTokenValue)
                .collect(Collectors.joining("\n"))
            : null;

    final AstNode requiresNode = node.getFirstChild(SwProductDefinitionGrammar.REQUIRES);
    final List<ProductUsage> usages =
        requiresNode != null
            ? requiresNode.getDescendants(SwProductDefinitionGrammar.PRODUCT_REF).stream()
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
