package nl.ramsolutions.sw.magik.languageserver;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.definitions.ModuleDefFileScanner;
import nl.ramsolutions.sw.definitions.ProductDefFileScanner;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisSettings;
import nl.ramsolutions.sw.magik.analysis.definitions.FilterableDefinitionKeeperAdapter;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.io.JsonDefinitionReader;
import nl.ramsolutions.sw.magik.analysis.definitions.io.JsonDefinitionWriter;
import nl.ramsolutions.sw.magik.analysis.indexer.MagikIndexer;
import nl.ramsolutions.sw.magik.analysis.indexer.ProductIndexer;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Magik definition workspace handler.
 *
 * <p>Handles things like: - updating definitions on file events - loading of existing type database
 * with definitions (on start up) - dumping type database with definitions (on shut down) - re-index
 * of existing definitions, in case of timestamp differences
 *
 * <p>One handler is to be instantiated per workspace.
 */
public class MagikWorkspaceFolder {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikWorkspaceFolder.class);
  private static final String TYPES_DB_FILENAME = "types.jsonl";

  private final WorkspaceFolder workspaceFolder;
  private final IDefinitionKeeper definitionKeeper;
  private final MagikToolsProperties languageServerProperties;
  private final IgnoreHandler ignoreHandler;
  private final ProductIndexer productIndexer;
  private final MagikIndexer magikIndexer;

  /**
   * Constructor.
   *
   * @param workspaceFolder Workspace folder.
   * @param definitionKeeper Definition keeper.
   * @param languageServerProperties Language server properties.
   */
  public MagikWorkspaceFolder(
      final WorkspaceFolder workspaceFolder,
      final IDefinitionKeeper definitionKeeper,
      final MagikToolsProperties languageServerProperties) {
    this.workspaceFolder = workspaceFolder;
    this.definitionKeeper = definitionKeeper;
    this.languageServerProperties = languageServerProperties;

    this.ignoreHandler = new IgnoreHandler();
    this.productIndexer = new ProductIndexer(this.definitionKeeper, this.ignoreHandler);
    this.magikIndexer =
        new MagikIndexer(this.definitionKeeper, this.languageServerProperties, this.ignoreHandler);
  }

  /**
   * Init handler.
   *
   * @throws IOException If an error occurs.
   */
  public void onInit() throws IOException {
    LOGGER.debug("On init: {}", this);

    final MagikAnalysisSettings settings = new MagikAnalysisSettings(this.languageServerProperties);
    if (settings.getTypingCacheIndexedDefinitions()) {
      this.readExistingTypesDatabase();
    }

    this.runIgnoreHandler();
    this.runProductIndexer();
    this.runMagikIndexer();

    LOGGER.debug("Done on init: {}", this);
  }

  /**
   * Shutdown handler.
   *
   * @throws IOException If an error occurs.
   */
  public void onShutdown() throws IOException {
    LOGGER.debug("On shutdown: {}", this);

    final MagikAnalysisSettings settings = new MagikAnalysisSettings(this.languageServerProperties);
    if (settings.getTypingCacheIndexedDefinitions()) {
      this.writeTypesDatabase();
    }

    LOGGER.debug("Done on shutdown: {}", this);
  }

  private void readExistingTypesDatabase() throws IOException {
    final Path workspacePath = this.getWorkspacePath();
    final Path typesDbPath = workspacePath.resolve(TYPES_DB_FILENAME);
    if (Files.exists(typesDbPath)) {
      LOGGER.debug("Reading types database for workspace: {}, path: {}", this, typesDbPath);
      JsonDefinitionReader.readTypes(typesDbPath, this.definitionKeeper);
    }
  }

  private void runIgnoreHandler() throws IOException {
    final URI uri = this.getWorkspaceUri();
    final FileEvent fileEvent = new FileEvent(uri, FileEvent.FileChangeType.CREATED);
    LOGGER.trace("Running IgnoreHandler for: {}", this);
    this.ignoreHandler.handleFileEvent(fileEvent);
  }

  private void runProductIndexer() throws IOException {
    LOGGER.debug("Running ProductIndexer for: {}", this);

    final Stream<Path> indexableFiles =
        this.getProductModuleDefFiles()
            .filter(
                path ->
                    path.toString().toLowerCase().endsWith(ProductDefFileScanner.SW_PRODUCT_DEF)
                        || path.toString()
                            .toLowerCase()
                            .endsWith(ModuleDefFileScanner.SW_MODULE_DEF));
    final FilterableDefinitionKeeperAdapter filteredDefinitionKeeper =
        this.getWorkspaceFilteredDefinitionKeeper();
    final Collection<FileEvent> fileEvents =
        this.buildFileEventsForDifferences(
            indexableFiles,
            filteredDefinitionKeeper.getProductDefinitions(),
            filteredDefinitionKeeper.getModuleDefinitions());
    LOGGER.debug("Product/module file event count: {}", fileEvents.size());
    for (final FileEvent fileEvent : fileEvents) {
      this.productIndexer.handleFileEvent(fileEvent);
    }
  }

  private void runMagikIndexer() throws IOException {
    LOGGER.debug("Running MagikIndexer for: {}", this);

    final Path workspaceFolderPath = this.getWorkspacePath();
    final Stream<Path> indexableFiles =
        this.ignoreHandler
            .getIndexableFiles(workspaceFolderPath)
            .filter(path -> path.toString().toLowerCase().endsWith(".magik"));
    final FilterableDefinitionKeeperAdapter filteredDefinitionKeeper =
        this.getWorkspaceFilteredDefinitionKeeper();
    final Collection<FileEvent> fileEvents =
        this.buildFileEventsForDifferences(
            indexableFiles, filteredDefinitionKeeper.getMagikFileDefinitions());

    LOGGER.debug("Magik file event count: {}", fileEvents.size());
    for (final FileEvent fileEvent : fileEvents) {
      this.magikIndexer.handleFileEvent(fileEvent);
    }
  }

  private void writeTypesDatabase() throws IOException {
    final Path workspacePath = this.getWorkspacePath();
    final Path typesDbPath = workspacePath.resolve(TYPES_DB_FILENAME);
    if (Files.exists(typesDbPath)) {
      Files.delete(typesDbPath);
    }

    LOGGER.debug("Writing types database for workspace: {}, path: {}", this, typesDbPath);
    final FilterableDefinitionKeeperAdapter filteredDefinitionKeeper =
        this.getWorkspaceFilteredDefinitionKeeper();
    JsonDefinitionWriter.write(typesDbPath, filteredDefinitionKeeper);
  }

  private FilterableDefinitionKeeperAdapter getWorkspaceFilteredDefinitionKeeper() {
    final String workspaceUriStr = this.getWorkspaceUri().toString();
    final Predicate<IDefinition> locationPred =
        def ->
            def.getLocation() != null
                && def.getLocation().getUri().toString().startsWith(workspaceUriStr);
    return new FilterableDefinitionKeeperAdapter(
        this.definitionKeeper,
        locationPred::test,
        locationPred::test,
        locationPred::test,
        locationPred::test,
        locationPred::test,
        locationPred::test,
        locationPred::test,
        locationPred::test,
        locationPred::test,
        locationPred::test);
  }

  @SafeVarargs
  private Collection<FileEvent> buildFileEventsForDifferences(
      final Stream<Path> filePaths, final Collection<? extends IDefinition>... definitions) {
    final Map<URI, Instant> definitionUris =
        Stream.of(definitions)
            .flatMap(Collection::stream)
            .map(def -> Map.entry(def.getLocation().getUri(), def.getTimestamp()))
            .collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (val0, val1) -> val0));

    // Get updates/deletes.
    final Set<FileEvent> updateDeleteFileEvents =
        definitionUris.entrySet().stream()
            .map(
                entry -> {
                  final URI uri = entry.getKey();
                  final Path path = Path.of(uri);
                  final Instant defTime = entry.getValue();
                  try {
                    if (!Files.exists(path)) {
                      return new FileEvent(uri, FileChangeType.DELETED);
                    }

                    // As the JsonDefinitionReader/JsonDefinitionWriter store in millis, trunate
                    // first.
                    final FileTime fileTime = Files.getLastModifiedTime(path);
                    final Instant fileTimeInstant = fileTime.toInstant();
                    if (!fileTimeInstant.equals(defTime)) {
                      return new FileEvent(uri, FileChangeType.CHANGED);
                    }
                  } catch (final IOException exception) {
                    LOGGER.debug("Error checking file: " + uri, exception);
                    exception.printStackTrace();
                  }

                  return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    // Get new files.
    final Set<URI> indexedFiles = definitionUris.keySet();
    final Set<FileEvent> newFileEvents =
        filePaths
            .filter(indexablePath -> !indexedFiles.contains(indexablePath.toUri()))
            .map(indexablePath -> new FileEvent(indexablePath.toUri(), FileChangeType.CREATED))
            .collect(Collectors.toSet());

    return Stream.concat(updateDeleteFileEvents.stream(), newFileEvents.stream())
        .collect(Collectors.toSet());
  }

  private URI getWorkspaceUri() {
    final String uriStr = this.workspaceFolder.getUri();
    return URI.create(uriStr);
  }

  private Path getWorkspacePath() {
    final URI uri = this.getWorkspaceUri();
    return Path.of(uri);
  }

  private Stream<Path> getProductModuleDefFiles() throws IOException {
    final Path workspacePath = this.getWorkspacePath();
    return Files.find(
        workspacePath,
        Integer.MAX_VALUE,
        (path, attributes) ->
            path.getFileName().toString().equalsIgnoreCase(ProductDefFileScanner.SW_PRODUCT_DEF)
                || path.getFileName()
                    .toString()
                    .equalsIgnoreCase(ModuleDefFileScanner.SW_MODULE_DEF));
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s, %s)",
        this.getClass().getName(),
        Integer.toHexString(this.hashCode()),
        this.workspaceFolder.getUri(),
        this.workspaceFolder.getName());
  }
}
