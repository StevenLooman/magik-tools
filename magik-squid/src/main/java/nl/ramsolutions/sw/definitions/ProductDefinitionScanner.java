package nl.ramsolutions.sw.definitions;

import com.sonar.sslr.api.AstNode;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.definitions.api.SwProductDefinitionGrammar;
import nl.ramsolutions.sw.definitions.parser.SwProductDefParser;
import nl.ramsolutions.sw.magik.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * product.def file scanner.
 */
public final class ProductDefinitionScanner {

    private static class ProductDefFileVisitor extends SimpleFileVisitor<Path> {

        private final Deque<ProductDefinition> stack = new ArrayDeque<>();
        private final Set<ProductDefinition> products = new HashSet<>();

        public Set<ProductDefinition> getProducts() {
            return Collections.unmodifiableSet(products);
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            final ProductDefinition parentProduct = this.stack.peek();

            final Path productDefPath = dir.resolve(SW_PRODUCT_DEF_PATH);
            final ProductDefinition currentProduct = Files.exists(productDefPath)
                ? this.addProduct(productDefPath)
                : parentProduct;
            this.stack.push(currentProduct);

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            this.stack.pop();

            return FileVisitResult.CONTINUE;
        }

        private ProductDefinition addProduct(final Path path) {
            final ProductDefinition parentProduct = this.stack.peek();

            try {
                final ProductDefinition currentProduct = ProductDefinitionScanner.readProductDefinition(path);
                this.products.add(currentProduct);

                if (parentProduct != null) {
                    final String currentProductName = currentProduct.getName();
                    parentProduct.addChild(currentProductName);
                }

                return currentProduct;
            } catch (final IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }

            return null;
        }

    }

    /**
     * Product definition filename.
     */
    public static final String SW_PRODUCT_DEF = "product.def";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductDefinitionScanner.class);
    private static final Path SW_PRODUCT_DEF_PATH = Path.of(ProductDefinitionScanner.SW_PRODUCT_DEF);

    private ProductDefinitionScanner() {
    }

    /**
     * Scan for products.
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
                return ProductDefinitionScanner.readProductDefinition(productDefPath);
            }

            path = path.getParent();
        }

        return null;
    }

    /**
     * Read product.def file.
     * @param path Path to {@code product.def} file.
     * @return Parsed product definition.
     * @throws IOException -
     */
    public static ProductDefinition readProductDefinition(final Path path) throws IOException {
        final SwProductDefParser parser = new SwProductDefParser();
        final AstNode node = parser.parse(path);

        final URI uri = path.toUri();
        final Location location = new Location(uri);

        final AstNode productIdentNode = node.getFirstChild(SwProductDefinitionGrammar.PRODUCT_IDENTIFICATION);
        final AstNode identfierNode = productIdentNode.getFirstChild(SwProductDefinitionGrammar.IDENTIFIER);
        final String productName = identfierNode.getTokenValue();

        final AstNode versionNode = node.getFirstChild(SwProductDefinitionGrammar.VERSION);
        final String version = versionNode != null
            ? versionNode.getFirstChild(SwProductDefinitionGrammar.VERSION_NUMBER).getTokenValue()
            : null;
        final String versionComment = versionNode != null
            ? versionNode.getFirstChild(SwProductDefinitionGrammar.REST_OF_LINE).getTokenValue()
            : null;

        return new ProductDefinition(location, productName, version, versionComment);
    }

}
