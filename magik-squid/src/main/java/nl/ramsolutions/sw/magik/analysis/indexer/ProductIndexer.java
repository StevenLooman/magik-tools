package nl.ramsolutions.sw.magik.analysis.indexer;

import com.sonar.sslr.api.RecognitionException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.definitions.ModuleDefinition;
import nl.ramsolutions.sw.definitions.ModuleDefinitionScanner;
import nl.ramsolutions.sw.definitions.ProductDefinition;
import nl.ramsolutions.sw.definitions.ProductDefinitionScanner;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Product (and module) definition indexer. */
public class ProductIndexer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductIndexer.class);

  private final IDefinitionKeeper definitionKeeper;
  private final IgnoreHandler ignoreHandler;
  private final Map<Path, ProductDefinition> indexedProducts = new HashMap<>();
  private final Map<Path, ModuleDefinition> indexedModules = new HashMap<>();

  public ProductIndexer(
      final IDefinitionKeeper definitionKeeper, final IgnoreHandler ignoreHandler) {
    this.definitionKeeper = definitionKeeper;
    this.ignoreHandler = ignoreHandler;
  }

  public synchronized void handleFileEvent(final FileEvent fileEvent) throws IOException {
    // Don't index if ignored.
    final URI uri = fileEvent.getUri();
    final Path path = Path.of(uri);
    if (this.ignoreHandler.isIgnored(path)) {
      return;
    }

    final FileChangeType fileChangeType = fileEvent.getFileChangeType();
    final List<Path> indexableFiles =
        fileChangeType == FileChangeType.DELETED
            ? this.getIndexedFiles(path).toList()
            : this.ignoreHandler
                .getIndexableFiles(path)
                .filter(indexablePath -> indexablePath.toString().toLowerCase().endsWith(".def"))
                .toList();
    switch (fileChangeType) {
      case DELETED:
        indexableFiles.forEach(this::indexPathDeleted);
        break;

      case CREATED:
        indexableFiles.forEach(this::indexPathCreated);
        break;

      case CHANGED:
        indexableFiles.forEach(this::indexPathChanged);
        break;

      default:
        throw new UnsupportedOperationException();
    }
  }

  private Stream<Path> getIndexedFiles(final Path path) {
    // Get all previously indexed files at or below path.
    return Stream.of(
            this.indexedProducts.entrySet().stream().map(Map.Entry::getKey),
            this.indexedModules.entrySet().stream().map(Map.Entry::getKey))
        .flatMap(stream -> stream)
        .filter(indexedPath -> indexedPath.startsWith(path));
  }

  /**
   * Index a single magik file when it is created (or first read).
   *
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
   *
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
   *
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
   *
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
      final String separator = path.getFileSystem().getSeparator();
      final Path parentPath = path.resolve(".." + separator + "..");
      final ProductDefinition parentDefinition =
          ProductDefinitionScanner.productForPath(parentPath);
      definition = ProductDefinitionScanner.readProductDefinition(path, parentDefinition);
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
   *
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
