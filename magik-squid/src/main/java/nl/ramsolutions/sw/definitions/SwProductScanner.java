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
import java.util.Deque;
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
public final class SwProductScanner {

    private static class RootSwProduct extends SwProductDefinition {

        RootSwProduct(final Location location) {
            super(location, "root", null, null);
        }

    }

    private static class ProductDefFileVisitor extends SimpleFileVisitor<Path> {

        private final Deque<SwProductDefinition> stack = new ArrayDeque<>();
        private final SwProductDefinition rootProduct;

        ProductDefFileVisitor(final Path path) {
            final URI uri = path.toUri();
            final Location location = new Location(uri);
            this.rootProduct = new RootSwProduct(location);
            this.stack.add(rootProduct);
        }

        public SwProductDefinition getRootProduct() {
            return this.rootProduct;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            final SwProductDefinition parentProduct = this.stack.peek();

            final Path productDefPath = dir.resolve(SW_PRODUCT_DEF_PATH);
            final SwProductDefinition currentProduct = Files.exists(productDefPath)
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

        private SwProductDefinition addProduct(final Path path) {
            final SwProductDefinition parentProduct = this.stack.peek();

            try {
                final SwProductDefinition currentProduct = SwProductScanner.readProductDefinition(path);
                if (parentProduct != null) {
                    parentProduct.addChild(currentProduct);
                }

                return currentProduct;
            } catch (IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }

            return null;
        }

    }

    /**
     * Product definition filename.
     */
    public static final String SW_PRODUCT_DEF = "product.def";

    private static final Logger LOGGER = LoggerFactory.getLogger(SwProductScanner.class);
    private static final Path SW_PRODUCT_DEF_PATH = Path.of(SwProductScanner.SW_PRODUCT_DEF);

    private SwProductScanner() {
    }

    /**
     * Scan for products.
     * @param path Path to scan from.
     * @return All found product definitions.
     * @throws IOException -
     */
    public static Set<SwProductDefinition> scanProducts(final Path path) throws IOException {
        final ProductDefFileVisitor fileVisitor = new ProductDefFileVisitor(path);
        Files.walkFileTree(path, fileVisitor);
        return fileVisitor.getRootProduct().getChildren();
    }

    /**
     * Get product from a given path, iterates upwards to find product.def file.
     * @param startPath Path to start at.
     * @return Parsed product definition.
     * @throws IOException -
     */
    @CheckForNull
    public static SwProductDefinition productForPath(final Path startPath) throws IOException {
        Path path = startPath;
        while (path != null) {
            final Path productDefPath = path.resolve(SW_PRODUCT_DEF_PATH);
            final File productDefFile = productDefPath.toFile();
            if (productDefFile.exists()) {
                return SwProductScanner.readProductDefinition(productDefPath);
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
    public static SwProductDefinition readProductDefinition(final Path path) throws IOException {
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

        return new SwProductDefinition(location, productName, version, versionComment);
    }

}
