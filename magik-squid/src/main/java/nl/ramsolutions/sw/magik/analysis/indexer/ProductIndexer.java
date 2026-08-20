package nl.ramsolutions.sw.magik.analysis.indexer;

import com.sonar.sslr.api.RecognitionException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.productdef.ProductDefFile;
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
      // Only drop what this indexer owns: every watched-file event reaches all indexers, and only
      // the owning one puts its definitions back.
      final Collection<IDefinition> definitions = this.definitionKeeper.getDefinitionsByPath(path);
      final List<ProductDefinition> ownDefinitions =
          definitions.stream()
              .filter(ProductDefinition.class::isInstance)
              .map(ProductDefinition.class::cast)
              .toList();
      ownDefinitions.forEach(this.definitionKeeper::remove);
    }

    if (fileChangeType == FileChangeType.CREATED || fileChangeType == FileChangeType.CHANGED) {
      final Path parentPath = path.getParent();
      final SourceFileScanner scanner =
          new SourceFileScanner(this.ignoreHandler, SourceFileScanner.PRODUCT_DEF_FILE_FILTER);
      scanner.getFiles(parentPath).forEach(this::indexFile);
    }

    LOGGER.debug("Handled file event: {}", fileEvent);
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
      this.readDefinitions(path);
    } catch (final Exception exception) {
      LOGGER.error("Error indexing created file: " + path, exception);
    }
  }

  private void readDefinitions(final Path path) throws IOException {
    final Path parentParentPath = path.resolve("..").resolve("..");
    final URI parentParentUri = parentParentPath.toUri();
    final ProductDefFile parentProductDefFile =
        ProductDefFile.getProductDefFileForUri(parentParentUri, this.definitionKeeper);
    final ProductDefinition definition;
    try {
      definition = parentProductDefFile.getProductDefinition();
    } catch (final RecognitionException exception) {
      LOGGER.warn("Error parsing definition at: " + path, exception);
      return;
    }

    final IDefinition bareDefinition = definition.getBareDefinition();
    this.definitionKeeper.add(bareDefinition);
  }
}
