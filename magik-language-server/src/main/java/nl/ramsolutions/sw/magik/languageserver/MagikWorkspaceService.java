package nl.ramsolutions.sw.magik.languageserver;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.io.JsonDefinitionReader;
import nl.ramsolutions.sw.magik.analysis.indexer.MagikIndexer;
import nl.ramsolutions.sw.magik.analysis.indexer.ProductIndexer;
import nl.ramsolutions.sw.magik.analysis.typing.ClassInfoDefinitionReader;
import nl.ramsolutions.sw.magik.languageserver.munit.MUnitTestItem;
import nl.ramsolutions.sw.magik.languageserver.munit.MUnitTestItemProvider;
import nl.ramsolutions.sw.magik.languageserver.symbol.SymbolProvider;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.WorkDoneProgressCreateParams;
import org.eclipse.lsp4j.WorkDoneProgressEnd;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Magik WorkspaceService. */
public class MagikWorkspaceService implements WorkspaceService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikWorkspaceService.class);

  private final MagikLanguageServer languageServer;
  private final MagikToolsProperties languageServerProperties;
  private final IDefinitionKeeper definitionKeeper;
  private final IgnoreHandler ignoreHandler;
  private final ProductIndexer productIndexer;
  private final MagikIndexer magikIndexer;
  private final SymbolProvider symbolProvider;
  private final MUnitTestItemProvider testItemProvider;

  /**
   * Constructor.
   *
   * @param languageServer Owner language server.
   * @param definitionKeeper {@link IDefinitionKeeper} used for definition storage.
   * @throws IOException
   */
  public MagikWorkspaceService(
      final MagikLanguageServer languageServer,
      final MagikToolsProperties languageServerProperties,
      final IDefinitionKeeper definitionKeeper) {
    this.languageServer = languageServer;
    this.languageServerProperties = languageServerProperties;
    this.definitionKeeper = definitionKeeper;

    this.ignoreHandler = new IgnoreHandler();
    this.productIndexer = new ProductIndexer(this.definitionKeeper, this.ignoreHandler);
    this.magikIndexer =
        new MagikIndexer(this.definitionKeeper, this.languageServerProperties, this.ignoreHandler);
    this.symbolProvider = new SymbolProvider(this.definitionKeeper);
    this.testItemProvider = new MUnitTestItemProvider(this.definitionKeeper);
  }

  /**
   * Set capabilities.
   *
   * @param capabilities Server capabilities to set.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    this.symbolProvider.setCapabilities(capabilities);
    this.testItemProvider.setCapabilities(capabilities);
  }

  @Override
  public void didChangeConfiguration(final DidChangeConfigurationParams params) {
    LOGGER.trace("didChangeConfiguration");

    final JsonObject settings = (JsonObject) params.getSettings();
    final Properties props = JsonObjectPropertiesConverter.convert(settings);
    LOGGER.debug("New properties: {}", props);
    this.languageServerProperties.reset();
    this.languageServerProperties.putAll(props);

    this.runIndexersInBackground();
  }

  private void runIgnoreFilesIndexer() {
    for (final WorkspaceFolder workspaceFolder : this.languageServer.getWorkspaceFolders()) {
      try {
        LOGGER.trace("Running IgnoreHandler from: {}", workspaceFolder.getUri());
        final String uriStr = workspaceFolder.getUri();
        final URI uri = URI.create(uriStr);
        final FileEvent fileEvent = new FileEvent(uri, FileEvent.FileChangeType.CREATED);
        this.ignoreHandler.handleFileEvent(fileEvent);
      } catch (final IOException exception) {
        LOGGER.error(exception.getMessage(), exception);
      }
    }
  }

  private void runProductIndexer() {
    for (final WorkspaceFolder workspaceFolder : this.languageServer.getWorkspaceFolders()) {
      try {
        LOGGER.debug("Running ProductIndexer from: {}", workspaceFolder.getUri());
        final String uriStr = workspaceFolder.getUri();
        final URI uri = URI.create(uriStr);
        final FileEvent fileEvent = new FileEvent(uri, FileEvent.FileChangeType.CREATED);
        this.productIndexer.handleFileEvent(fileEvent);
      } catch (final IOException exception) {
        LOGGER.error(exception.getMessage(), exception);
      }
    }
  }

  private void runMagikIndexer() {
    for (final WorkspaceFolder workspaceFolder : this.languageServer.getWorkspaceFolders()) {
      try {
        LOGGER.debug("Running MagikIndexer from: {}", workspaceFolder.getUri());
        final String uriStr = workspaceFolder.getUri();
        final URI uri = URI.create(uriStr);
        final FileEvent fileEvent = new FileEvent(uri, FileEvent.FileChangeType.CREATED);
        this.magikIndexer.handleFileEvent(fileEvent);
      } catch (final IOException exception) {
        LOGGER.error(exception.getMessage(), exception);
      }
    }
  }

  private void readLibsClassInfos(final List<String> libsDirs) {
    LOGGER.trace("Reading libs docs from: {}", libsDirs);

    libsDirs.forEach(
        pathStr -> {
          final Path path = Path.of(pathStr);
          if (!Files.exists(path)) {
            LOGGER.warn("Path to libs dir does not exist: {}", pathStr);
            return;
          }

          try {
            ClassInfoDefinitionReader.readLibsDirectory(path, this.definitionKeeper);
          } catch (final IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
          }
        });
  }

  /**
   * Read the type databases from the given path.
   *
   * @param typeDbPaths Paths to type databases.
   */
  public void readTypesDbs(final List<String> typeDbPaths) {
    LOGGER.trace("Reading type databases from: {}", typeDbPaths);

    typeDbPaths.forEach(
        pathStr -> {
          final Path path = Path.of(pathStr);
          if (!Files.exists(path)) {
            LOGGER.warn("Path to types database does not exist: {}", pathStr);
            return;
          }

          try {
            JsonDefinitionReader.readTypes(path, this.definitionKeeper);
          } catch (final IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
          }
        });
  }

  @Override
  public void didChangeWatchedFiles(final DidChangeWatchedFilesParams params) {
    params.getChanges().stream()
        .forEach(
            fileEvent -> {
              LOGGER.debug(
                  "File event: uri: {}, type: {}", fileEvent.getUri(), fileEvent.getType());

              final FileChangeType fileChangeType = fileEvent.getType();
              final URI uri = URI.create(fileEvent.getUri());
              final Path path = Path.of(uri);
              if (fileChangeType != FileChangeType.Deleted && !Files.exists(path)) {
                // Ensure file still exists. Files such as `.git/index.lock` are often already
                // deleted before it reaches this method.
                return;
              }

              final nl.ramsolutions.sw.magik.FileEvent.FileChangeType magikFileChangeType =
                  Lsp4jConversion.fileChangeTypeFromLsp4j(fileChangeType);
              final nl.ramsolutions.sw.magik.FileEvent magikFileEvent =
                  new nl.ramsolutions.sw.magik.FileEvent(uri, magikFileChangeType);
              try {
                this.ignoreHandler.handleFileEvent(magikFileEvent);
                this.productIndexer.handleFileEvent(magikFileEvent);
                this.magikIndexer.handleFileEvent(magikFileEvent);
              } catch (final IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
              }
            });
  }

  @Override
  public CompletableFuture<
          Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>>
      symbol(WorkspaceSymbolParams params) {
    final String query = params.getQuery();
    LOGGER.trace("symbol, query: {}", query);

    return CompletableFuture.supplyAsync(
        () -> {
          final List<WorkspaceSymbol> queryResults = this.symbolProvider.getSymbols(query);
          LOGGER.debug("Symbols found for: '{}', count: {}", query, queryResults.size());
          return Either.forRight(queryResults);
        });
  }

  // region: Additional commands.
  /**
   * Re-index all magik files.
   *
   * @return CompletableFuture.
   */
  @JsonRequest(value = "custom/reIndex")
  public CompletableFuture<Void> reIndex() {
    return CompletableFuture.runAsync(
        () -> {
          this.definitionKeeper.clear();

          this.runIndexersInBackground();
        });
  }

  /**
   * Get test items.
   *
   * @return Test items.
   */
  @JsonRequest(value = "custom/munit/getTestItems")
  public CompletableFuture<Collection<MUnitTestItem>> getTestItems() {
    // TODO: Rewrite this to generic queries on types. Such as:
    //       - Get type by name
    //         - doc
    //         - location
    //         - parents
    //         - children
    //         - ...
    //         - methods?
    //       - Get methods from type name
    //       - Get method by name
    //       In fact, maybe we can use LSP typeHierarchy support?
    LOGGER.trace("munit/getTestItems");

    return CompletableFuture.supplyAsync(this.testItemProvider::getTestItems);
  }

  // endregion

  private void runIndexers() {
    LOGGER.trace("Run indexers");

    // Read types db.
    final MagikLanguageServerSettings settings =
        new MagikLanguageServerSettings(this.languageServerProperties);
    final List<String> typesDbPaths = settings.getTypingTypeDatabasePaths();
    this.readTypesDbs(typesDbPaths);

    // Read class_infos from libs/ dirs.
    final List<String> libsDirs = settings.getLibsDirs();
    this.readLibsClassInfos(libsDirs);

    // Index .magik-tools-ignore files.
    this.runIgnoreFilesIndexer();

    // Run product/module indexer.
    this.runProductIndexer();

    // Run magik indexer.
    this.runMagikIndexer();
  }

  @SuppressWarnings("IllegalCatch")
  private void runIndexersInBackground() {
    LOGGER.trace("Run background indexer");

    final LanguageClient languageClient = this.languageServer.getLanguageClient();
    final WorkDoneProgressCreateParams params = new WorkDoneProgressCreateParams();
    final String token = UUID.randomUUID().toString();
    params.setToken(token);
    languageClient.createProgress(params);

    CompletableFuture.runAsync(
        () -> {
          LOGGER.trace("Start indexing workspace in background");
          final ProgressParams progressParams = new ProgressParams();
          progressParams.setToken(token);

          final WorkDoneProgressBegin begin = new WorkDoneProgressBegin();
          begin.setTitle("Indexing workspace");
          progressParams.setValue(Either.forLeft(begin));
          languageClient.notifyProgress(progressParams);

          try {
            this.runIndexers();
          } catch (final Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
          }

          final WorkDoneProgressEnd end = new WorkDoneProgressEnd();
          end.setMessage("Done indexing workspace");
          progressParams.setValue(Either.forLeft(end));
          languageClient.notifyProgress(progressParams);
          LOGGER.trace("Done indexing workspace in background");
        });
  }

  public void shutdown() {
    // TODO: Dump type database, and read it again when starting?
    //       Requires timestamping of definitions/files!
  }
}
