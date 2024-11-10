package nl.ramsolutions.sw.magik.analysis.indexer;

import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.MagikFileScanner;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Magik file indexer. */
public class MagikIndexer {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikIndexer.class);

  private final IDefinitionKeeper definitionKeeper;
  private final MagikToolsProperties properties;
  private final IgnoreHandler ignoreHandler;

  /**
   * Constructor.
   *
   * @param definitionKeeper {@link DefinitionKeeper} to write to.
   * @param properties {@link MagikToolsProperties} to use.
   * @param ignoreHandler {@link IgnoreHandler} to check if files are ignored.
   */
  public MagikIndexer(
      final IDefinitionKeeper definitionKeeper,
      final MagikToolsProperties properties,
      final IgnoreHandler ignoreHandler) {
    this.definitionKeeper = definitionKeeper;
    this.properties = properties;
    this.ignoreHandler = ignoreHandler;
  }

  /**
   * Handle file event.
   *
   * @param fileEvent {@link FileEvent} to handle.
   * @throws IOException If an error occurs.
   */
  public synchronized void handleFileEvent(final FileEvent fileEvent) throws IOException {
    LOGGER.debug("Handling file event: {}", fileEvent);

    final Path path = fileEvent.getPath();
    final FileChangeType fileChangeType = fileEvent.getFileChangeType();
    if (fileChangeType == FileChangeType.CHANGED || fileChangeType == FileChangeType.DELETED) {
      this.definitionKeeper.getDefinitionsByPath(path).forEach(this.definitionKeeper::remove);
    }

    if (fileChangeType == FileChangeType.CREATED || fileChangeType == FileChangeType.CHANGED) {
      final MagikFileScanner scanner = new MagikFileScanner(this.ignoreHandler);
      scanner.getFiles(path).forEach(this::indexFile);
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
    LOGGER.debug("Indexing created/updated file: {}", path);

    try {
      this.readDefinitions(path);
    } catch (final Exception exception) {
      LOGGER.error("Error indexing created file: " + path, exception);
    }
  }

  /**
   * Read definitions from path.
   *
   * @param path Path to magik file.
   */
  private void readDefinitions(final Path path) {
    try {
      final MagikFile magikFile = new MagikFile(this.properties, path);
      magikFile.getDefinitions().stream()
          .map(IDefinition::getBareDefinition)
          .forEach(this.definitionKeeper::add);
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }
  }
}
