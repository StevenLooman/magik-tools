package nl.ramsolutions.sw.magik.analysis.indexer;

import com.sonar.sslr.api.RecognitionException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefFileScanner;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFileScanner;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Product definition indexer. */
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

    final FileChangeType fileChangeType = fileEvent.getFileChangeType();
    final Path path = fileEvent.getPath();
    if (fileChangeType == FileChangeType.CHANGED || fileChangeType == FileChangeType.DELETED) {
      this.getIndexedDefinitions(path).forEach(this::removeDefinition);
    }

    if (fileChangeType == FileChangeType.CREATED || fileChangeType == FileChangeType.CHANGED) {
      final ProductDefFileScanner productDefFileScanner =
          new ProductDefFileScanner(this.ignoreHandler);
      final ProductDefFileScanner.Tree productDefTree = productDefFileScanner.getProductTree(path);
      if (productDefTree != null) {
        productDefTree.stream().forEach(this::indexFile);
      }
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
    // TODO: ProductDefFileDefinitions, like MagikFileDefinition?
    return this.definitionKeeper.getProductDefinitions().stream()
        .filter(def -> def.getLocation() != null && def.getLocation().getPath().startsWith(path))
        .collect(Collectors.toSet());
  }

  /**
   * Index a single magik file when it is created (or first read).
   *
   * @param path Path to magik file.
   */
  @SuppressWarnings("checkstyle:IllegalCatch")
  private void indexFile(final ProductDefFileScanner.Tree productDefTree) {
    final Path path = productDefTree.getPath();
    LOGGER.debug("Scanning created file: {}", path);

    try {
      this.readProductDefinition(path);
    } catch (final Exception exception) {
      LOGGER.error("Error indexing created file: " + path, exception);
    }
  }

  private void removeDefinition(final IDefinition definition) {
    if (definition instanceof ProductDefinition productDefinition) {
      this.definitionKeeper.remove(productDefinition);
    } else {
      throw new IllegalStateException("Unknown type");
    }
  }

  private void readProductDefinition(final Path path) throws IOException {
    final ProductDefinition definition;
    final Path parentPath = path.resolve("..").resolve("..");
    final Path productDefPath = ModuleDefFileScanner.getProductDefFileForPath(parentPath);
    final ProductDefFile parentProductDefFile;
    if (productDefPath != null) {
      parentProductDefFile = new ProductDefFile(productDefPath, this.definitionKeeper, null);
    } else {
      parentProductDefFile = null;
    }
    try {
      final ProductDefFile productDefFile =
          new ProductDefFile(path, this.definitionKeeper, parentProductDefFile);
      definition = productDefFile.getProductDefinition();
    } catch (final RecognitionException exception) {
      LOGGER.warn("Error parsing defintion at: " + path, exception);
      return;
    }

    this.definitionKeeper.add(definition);
  }
}
