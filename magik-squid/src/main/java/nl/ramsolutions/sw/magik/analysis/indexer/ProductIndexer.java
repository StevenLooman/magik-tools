package nl.ramsolutions.sw.magik.analysis.indexer;

import com.sonar.sslr.api.RecognitionException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.definitions.ModuleDefFileScanner;
import nl.ramsolutions.sw.definitions.ModuleDefinition;
import nl.ramsolutions.sw.definitions.ProductDefFileScanner;
import nl.ramsolutions.sw.definitions.ProductDefinition;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.ModuleDefFile;
import nl.ramsolutions.sw.magik.ProductDefFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Product (and module) definition indexer. */
public class ProductIndexer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductIndexer.class);

  private final IDefinitionKeeper definitionKeeper;
  private final IgnoreHandler ignoreHandler;

  public ProductIndexer(
      final IDefinitionKeeper definitionKeeper, final IgnoreHandler ignoreHandler) {
    this.definitionKeeper = definitionKeeper;
    this.ignoreHandler = ignoreHandler;
  }

  /**
   * Handle file event.
   *
   * @param fileEvent {@link FileEvent} to handle.
   * @throws IOException -
   */
  public synchronized void handleFileEvent(final FileEvent fileEvent) throws IOException {
    LOGGER.debug("Handling file event: {}", fileEvent);

    // Don't index if ignored.
    final Path path = fileEvent.getPath();
    if (this.ignoreHandler.isIgnored(path)) {
      LOGGER.debug("Handled file event: {} (ignored)", fileEvent);
      return;
    }

    final FileChangeType fileChangeType = fileEvent.getFileChangeType();
    if (fileChangeType == FileChangeType.CHANGED || fileChangeType == FileChangeType.DELETED) {
      this.getIndexedDefinitions(path).forEach(this::removeDefinition);
    }

    if (fileChangeType == FileChangeType.CREATED || fileChangeType == FileChangeType.CHANGED) {
      final List<Path> indexableFiles = this.ignoreHandler.getIndexableFiles(path).toList();
      indexableFiles.forEach(this::indexFile);
    }

    LOGGER.debug("Handled file event: {}", fileEvent);
  }

  /**
   * Get all indexed definitions from path or lower.
   *
   * <p>Used when a directory is deleted or renamed, since we only get the delete of the directory
   * itself, not the individual files within the directory or sub-directories.
   *
   * @param path Path to search from.
   * @return Indexed definitions.
   */
  private Collection<IDefinition> getIndexedDefinitions(final Path path) {
    return Stream.of(
            this.definitionKeeper.getProductDefinitions(),
            this.definitionKeeper.getModuleDefinitions())
        .flatMap(Collection::stream)
        .filter(def -> def.getLocation() != null && def.getLocation().getPath().startsWith(path))
        .collect(Collectors.toSet());
  }

  /**
   * Index a single magik file when it is created (or first read).
   *
   * @param path Path to magik file.
   */
  @SuppressWarnings("checkstyle:IllegalCatch")
  private void indexFile(final Path path) {
    LOGGER.debug("Scanning created file: {}", path);

    try {
      if (path.endsWith(ProductDefFileScanner.SW_PRODUCT_DEF)) {
        this.readProductDefinition(path);
      } else if (path.endsWith(ModuleDefFileScanner.SW_MODULE_DEF)) {
        this.readModuleDefinition(path);
      }
    } catch (final Exception exception) {
      LOGGER.error("Error indexing created file: " + path, exception);
    }
  }

  private void removeDefinition(final IDefinition definition) {
    if (definition instanceof ProductDefinition productDefinition) {
      this.definitionKeeper.remove(productDefinition);
    } else if (definition instanceof ModuleDefinition moduleDefinition) {
      this.definitionKeeper.remove(moduleDefinition);
    }
  }

  private void readProductDefinition(final Path path) throws IOException {
    final ProductDefinition definition;
    try {
      final Path parentPath = path.resolve("..").resolve("..");

      final ProductDefFile parentDefinition =
          ProductDefFileScanner.getProductDefFileForPath(parentPath);
      final ProductDefFile productDefFile =
          new ProductDefFile(path, this.definitionKeeper, parentDefinition);
      definition = productDefFile.getProductDefinition();
    } catch (final RecognitionException exception) {
      LOGGER.warn("Error parsing defintion at: " + path, exception);
      return;
    }

    this.definitionKeeper.add(definition);
  }

  private void readModuleDefinition(final Path path) throws IOException {
    final ModuleDefinition definition;
    try {
      final ProductDefFile productDefFile = ProductDefFileScanner.getProductDefFileForPath(path);
      final ModuleDefFile moduleDefFile =
          new ModuleDefFile(path, this.definitionKeeper, productDefFile);
      definition = moduleDefFile.getModuleDefinition();
    } catch (final RecognitionException exception) {
      LOGGER.warn("Error parsing defintion at: " + path, exception);
      return;
    }

    this.definitionKeeper.add(definition);
  }
}
