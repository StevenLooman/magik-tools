package nl.ramsolutions.sw.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.definitions.api.SwProductDefinitionGrammar;
import nl.ramsolutions.sw.definitions.parser.SwProductDefParser;
import nl.ramsolutions.sw.magik.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** product.def file scanner. */
public final class ProductDefinitionScanner {

  private static final class ProductDefFileVisitor extends SimpleFileVisitor<Path> {

    private final Deque<ProductDefinition> stack = new ArrayDeque<>();
    private final Set<ProductDefinition> products = new HashSet<>();

    public Set<ProductDefinition> getProducts() {
      return Collections.unmodifiableSet(products);
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
        throws IOException {
      final ProductDefinition parentProduct = this.stack.peek();

      final Path productDefPath = dir.resolve(SW_PRODUCT_DEF_PATH);
      final ProductDefinition currentProduct =
          Files.exists(productDefPath) ? this.addProduct(productDefPath) : parentProduct;
      this.stack.push(currentProduct);

      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
        throws IOException {
      this.stack.pop();

      return FileVisitResult.CONTINUE;
    }

    private ProductDefinition addProduct(final Path path) {
      final ProductDefinition parentProduct = this.stack.peek();

      try {
        final ProductDefinition currentProduct =
            ProductDefinitionScanner.readProductDefinition(path, parentProduct);
        this.products.add(currentProduct);

        return currentProduct;
      } catch (final IOException exception) {
        LOGGER.error(exception.getMessage(), exception);
      }

      return null;
    }
  }

  /** Product definition filename. */
  public static final String SW_PRODUCT_DEF = "product.def";

  private static final String UNDEFINED_PRODUCT_NAME = "_undefined_product";

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductDefinitionScanner.class);
  private static final Path SW_PRODUCT_DEF_PATH = Path.of(ProductDefinitionScanner.SW_PRODUCT_DEF);

  private ProductDefinitionScanner() {}

  /**
   * Scan for products.
   *
   * @param path Path to scan from.
   * @return All found product definitions.
   * @throws IOException -
   */
  public static Set<ProductDefinition> scanProducts(final Path path) throws IOException {
    final ProductDefFileVisitor fileVisitor = new ProductDefFileVisitor();
    Files.walkFileTree(path, fileVisitor);
    return fileVisitor.getProducts();
  }

  /**
   * Get product from a given path, iterates upwards to find product.def file.
   *
   * <p>Note that this does *NOT* set the parent product.
   *
   * @param startPath Path to start at.
   * @return Parsed product definition.
   * @throws IOException -
   */
  @CheckForNull
  public static ProductDefinition productForPath(final Path startPath) throws IOException {
    Path path = startPath;
    while (path != null) {
      final Path productDefPath = path.resolve(SW_PRODUCT_DEF_PATH);
      final File productDefFile = productDefPath.toFile();
      if (productDefFile.exists()) {
        return ProductDefinitionScanner.readProductDefinition(productDefPath, null);
      }

      path = path.getParent();
    }

    return null;
  }

  /**
   * Read product.def file.
   *
   * @param path Path to {@code product.def} file.
   * @return Parsed product definition.
   * @throws IOException -
   */
  public static ProductDefinition readProductDefinition(
      final Path path, final @Nullable ProductDefinition parentProduct) throws IOException {
    final SwProductDefParser parser = new SwProductDefParser();
    final AstNode node = parser.parse(path);

    final URI uri = path.toUri();
    final Location location = new Location(uri);

    final Instant timestamp = Files.getLastModifiedTime(path).toInstant();

    final String productName;
    final AstNode productIdentNode =
        node.getFirstChild(SwProductDefinitionGrammar.PRODUCT_IDENTIFICATION);
    if (productIdentNode != null) {
      final AstNode nameNode =
          productIdentNode.getFirstChild(SwProductDefinitionGrammar.PRODUCT_NAME);
      productName = nameNode.getTokenValue();
    } else {
      productName = ProductDefinitionScanner.UNDEFINED_PRODUCT_NAME;
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
    final List<String> requireds =
        requiresNode != null
            ? requiresNode.getDescendants(SwProductDefinitionGrammar.PRODUCT_REF).stream()
                .map(AstNode::getTokenValue)
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
        requireds);
  }
}
