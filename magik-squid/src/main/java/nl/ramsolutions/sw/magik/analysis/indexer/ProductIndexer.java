package nl.ramsolutions.sw.magik.analysis.indexer;

import com.sonar.sslr.api.RecognitionException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import nl.ramsolutions.sw.definitions.ModuleDefinition;
import nl.ramsolutions.sw.definitions.ModuleDefinitionScanner;
import nl.ramsolutions.sw.definitions.ProductDefinition;
import nl.ramsolutions.sw.definitions.ProductDefinitionScanner;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Product (and module) definition indexer.
 */
public class ProductIndexer {

    // TODO: This should not be in typing.

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductIndexer.class);

    private final IDefinitionKeeper definitionKeeper;
    private final Map<Path, ProductDefinition> indexedProducts = new HashMap<>();
    private final Map<Path, ModuleDefinition> indexedModules = new HashMap<>();

    public ProductIndexer(final IDefinitionKeeper definitionKeeper) {
        this.definitionKeeper = definitionKeeper;
    }

    /**
     * Index all magik file(s).
     * @param paths Paths to index.
     * @throws IOException -
     */
    public void indexPaths(final Stream<Path> paths) throws IOException {
        paths
            .filter(path -> path.getFileName() != null)
            .filter(path ->
                path.getFileName().toString().equalsIgnoreCase("product.def")
                || path.getFileName().toString().equalsIgnoreCase("module.def"))
            .forEach(this::indexPathCreated);
    }

    /**
     * Index a single magik file when it is created (or first read).
     * @param path Path to magik file.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void indexPathCreated(final Path path) {
        LOGGER.debug("Scanning created file: {}", path);

        try {
            this.scrubDefinition(path);
            this.readDefinition(path);
        } catch (final Exception exception) {
            LOGGER.error("Error indexing created file: " + path, exception);
        }
    }

    /**
     * Index a single magik file when it is changed.
     * @param path Path to magik file.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void indexPathChanged(final Path path) {
        LOGGER.debug("Scanning changed file: {}", path);

        try {
            this.scrubDefinition(path);
            this.readDefinition(path);
        } catch (final Exception exception) {
            LOGGER.error("Error indexing changed file: " + path, exception);
        }
    }

    /**
     * Un-index a single magik file when it is deleted.
     * @param path Path to magik file.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void indexPathDeleted(final Path path) {
        LOGGER.debug("Scanning deleted file: {}", path);

        try {
            this.scrubDefinition(path);
        } catch (final Exception exception) {
            LOGGER.error("Error indexing deleted file: " + path, exception);
        }
    }

    /**
     * Read definitions from path.
     * @param path Path to magik file.
     */
    private void readDefinition(final Path path) {
        final Path filename = path.getFileName();
        try {
            if (filename.toString().equalsIgnoreCase("product.def")) {
                this.readProductDefinition(path);
            } else if (filename.toString().equalsIgnoreCase("module.def")) {
                this.readModuleDefinition(path);
            } else {
                throw new IllegalArgumentException();
            }
        } catch (final IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
    }

    private void readProductDefinition(final Path path) throws IOException {
        final ProductDefinition definition;
        try {
            definition = ProductDefinitionScanner.readProductDefinition(path);
        } catch (final RecognitionException exception) {
            LOGGER.warn("Error parsing defintion at: {}", path);
            return;
        }

        this.definitionKeeper.add(definition);
        this.indexedProducts.put(path, definition);
    }

    private void readModuleDefinition(final Path path) throws IOException {
        final ModuleDefinition definition;
        try {
            definition = ModuleDefinitionScanner.readModuleDefinition(path);
        } catch (final RecognitionException exception) {
            LOGGER.warn("Error parsing defintion at: {}", path);
            return;
        }

        this.definitionKeeper.add(definition);
        this.indexedModules.put(path, definition);
    }

    /**
     * Scrub definitions.
     * @param path Path to magik file.
     */
    private void scrubDefinition(final Path path) {
        if (this.indexedProducts.containsKey(path)) {
            final ProductDefinition definition = this.indexedProducts.get(path);
            this.definitionKeeper.remove(definition);

            this.indexedProducts.remove(path);
        }

        if (this.indexedModules.containsKey(path)) {
            final ModuleDefinition definition = this.indexedModules.get(path);
            this.definitionKeeper.remove(definition);

            this.indexedModules.remove(path);
        }
    }

}
