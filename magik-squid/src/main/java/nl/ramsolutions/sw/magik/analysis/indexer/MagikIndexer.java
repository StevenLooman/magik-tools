package nl.ramsolutions.sw.magik.analysis.indexer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefinition;
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
      // Only drop what this indexer owns: every watched-file event reaches all indexers, and only
      // the owning one puts its definitions back. Product/module definitions come from
      // product.def/module.def, which this indexer never reads.
      final Collection<IDefinition> definitions = this.definitionKeeper.getDefinitionsByPath(path);
      final List<IDefinition> ownDefinitions =
          definitions.stream().filter(MagikIndexer::isMagikOwned).toList();
      ownDefinitions.forEach(this.definitionKeeper::remove);
    }

    if (fileChangeType == FileChangeType.CREATED || fileChangeType == FileChangeType.CHANGED) {
      final SourceFileScanner scanner =
          new SourceFileScanner(ignoreHandler, SourceFileScanner.MAGIK_FILE_FILTER);
      scanner.getFiles(path).forEach(this::indexFile);
    }

    LOGGER.debug("Handled file event: {}", fileEvent);
  }

  /** Whether {@code definition} is one this indexer produces, and so may remove. */
  private static boolean isMagikOwned(final IDefinition definition) {
    return !(definition instanceof ProductDefinition) && !(definition instanceof ModuleDefinition);
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
   * Index the content of a single magik file from an in-memory text string.
   *
   * <p>Used when the file is open in the editor: the in-memory text takes priority over the on-disk
   * content.
   *
   * @param uri URI of the magik file.
   * @param text Current text content of the file.
   */
  public synchronized void indexFileContent(final URI uri, final String text) {
    LOGGER.debug("Indexing file content for: {}", uri);

    final Path path = Path.of(uri);

    // Parse before touching the keeper: the parse is by far the longest part of a re-index, and
    // removing first would leave the file's definitions absent for its whole duration.
    final MagikFile magikFile = new MagikFile(this.properties, uri, text);
    final List<IDefinition> definitions =
        magikFile.getDefinitions().stream().map(IDefinition::getBareDefinition).toList();

    this.definitionKeeper.replaceDefinitionsForPath(path, definitions);
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
