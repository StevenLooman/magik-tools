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
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisSettings;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.io.JsonDefinitionReader;
import nl.ramsolutions.sw.magik.analysis.indexer.MagikIndexer;
import nl.ramsolutions.sw.magik.analysis.indexer.ModuleIndexer;
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
  private static final Logger LOGGER_DURATION =
      LoggerFactory.getLogger(MagikWorkspaceService.class.getName() + "Duration");

  private final MagikLanguageServer languageServer;
  private final MagikToolsProperties languageServerProperties;
  private final IDefinitionKeeper definitionKeeper;
  private final IgnoreHandler ignoreHandler;
  private final ProductIndexer productIndexer;
  private final ModuleIndexer moduleIndexer;
  private final MagikIndexer magikIndexer;
  private final SymbolProvider symbolProvider;
  private final MUnitTestItemProvider testItemProvider;

  /**
   * Constructor.
   *
   * @param languageServer Owner language server.
   * @param definitionKeeper {@link IDefinitionKeeper} used for definition storage.
   * @throws IOException If an error occurs.
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
    this.moduleIndexer = new ModuleIndexer(this.definitionKeeper, this.ignoreHandler);
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

    final MagikToolsProperties oldProperties =
        new MagikToolsProperties(this.languageServerProperties);

    final JsonObject settings = (JsonObject) params.getSettings();
    final Properties props = JsonObjectPropertiesConverter.convert(settings);
    LOGGER.debug("New properties: {}", props);
    this.languageServerProperties.reset();
    this.languageServerProperties.putAll(props);

    // Only reindex if settings changed that require reindexing.
    // Assume that if there were no old settings, we are starting up/this is the first time settings
    // are set, so no reindexing is required as settings are the same as last session, and we can
    // use any caches.
    final MagikAnalysisSettings oldSettings = new MagikAnalysisSettings(oldProperties);
    final MagikAnalysisSettings newSettings =
        new MagikAnalysisSettings(this.languageServerProperties);
    if (oldSettings.isEmpty()) {
      this.runIndexersInBackground(false, true);
    } else if (newSettings.requiresReindexing(oldSettings)) {
      LOGGER.info("Settings changed that require full reindexing");
      this.runIndexersInBackground(true, false);
    }
  }

  private void readProductsClassInfos(final List<String> productDirs) {
    LOGGER.trace("Reading docs from product dirs: {}", productDirs);

    productDirs.forEach(
        pathStr -> {
          final Path path = Path.of(pathStr);
          if (!Files.exists(path)) {
            LOGGER.warn("Path to product dir does not exist: {}", pathStr);
            return;
          }

          try {
            ClassInfoDefinitionReader.readProductDirectory(path, this.definitionKeeper);
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
                this.productIndexer.handleFileEvent(magikFileEvent);
                this.moduleIndexer.handleFileEvent(magikFileEvent);
                this.magikIndexer.handleFileEvent(magikFileEvent);
              } catch (final IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
              }
            });
  }

  @Override
  public CompletableFuture<
          Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>>
      symbol(final WorkspaceSymbolParams params) {
    final long start = System.nanoTime();

    final String query = params.getQuery();
    LOGGER.trace("symbol, query: {}", query);

    return CompletableFuture.supplyAsync(
        () -> {
          final List<WorkspaceSymbol> queryResults = this.symbolProvider.getSymbols(query);
          LOGGER.debug("Symbols found for: '{}', count: {}", query, queryResults.size());

          if (LOGGER_DURATION.isTraceEnabled()) {
            LOGGER_DURATION.trace(
                "Duration: {} symbol, query: {}",
                "%.3f".formatted((System.nanoTime() - start) / 1000000000.0),
                query);
          }

          return Either.forRight(queryResults);
        });
  }

  // region: Additional commands.
  /**
   * Re-index all files.
   *
   * @return CompletableFuture.
   */
  @JsonRequest(value = "custom/reIndex")
  public CompletableFuture<Void> reIndex() {
    return CompletableFuture.runAsync(
        () -> {
          this.runIndexersInBackground(true, true);
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

  @SuppressWarnings("IllegalCatch")
  private void runIndexersInBackground(
      final boolean doCleanTypeDatabases, final boolean readTypesDbs) {
    LOGGER.trace("Run background indexer");

    final LanguageClient languageClient = this.languageServer.getLanguageClient();
    final WorkDoneProgressCreateParams params = new WorkDoneProgressCreateParams();
    final String token = UUID.randomUUID().toString();
    params.setToken(token);
    languageClient.createProgress(params);

    CompletableFuture.runAsync(
        () -> {
          LOGGER.trace("Start indexing workspace");
          final ProgressParams progressParams = new ProgressParams();
          progressParams.setToken(token);

          final WorkDoneProgressBegin begin = new WorkDoneProgressBegin();
          begin.setTitle("Magik: Indexing workspace");
          progressParams.setValue(Either.forLeft(begin));
          languageClient.notifyProgress(progressParams);

          try {
            if (doCleanTypeDatabases) {
              this.cleanTypeDatabases();
            }

            this.runIndexers(doCleanTypeDatabases || readTypesDbs);
          } catch (final Exception exception) {
            LOGGER.error(exception.getMessage(), exception);
          }

          final WorkDoneProgressEnd end = new WorkDoneProgressEnd();
          end.setMessage("Magik: Done indexing workspace");
          progressParams.setValue(Either.forLeft(end));
          languageClient.notifyProgress(progressParams);
          LOGGER.trace("Done indexing workspace in background");
        });
  }

  private void cleanTypeDatabases() {
    LOGGER.trace("Clean type databases");

    this.definitionKeeper.clear();

    // Clean workspace folders of cached data.
    for (final MagikWorkspaceFolder workspaceFolder : this.languageServer.getWorkspaceFolders()) {
      try {
        workspaceFolder.clean();
      } catch (final IOException exception) {
        LOGGER.error("Caught error when cleaning workspacefolder: " + workspaceFolder, exception);
      }
    }
  }

  private void runIndexers(final boolean readTypesDbs) {
    LOGGER.trace("Run indexers");

    if (readTypesDbs) {
      // Read types dbs.
      final MagikLanguageServerSettings settings =
          new MagikLanguageServerSettings(this.languageServerProperties);
      final List<String> typesDbPaths = settings.getTypingTypeDatabasePaths();
      this.readTypesDbs(typesDbPaths);

      // Read class_infos from product dirs.
      final List<String> productDirs = settings.getProductDirs();
      this.readProductsClassInfos(productDirs);
    }

    // Update workspace folders.
    for (final MagikWorkspaceFolder workspaceFolder : this.languageServer.getWorkspaceFolders()) {
      try {
        workspaceFolder.onInit();
      } catch (final IOException exception) {
        LOGGER.error(
            "Caught error when initializing workspacefolder: " + workspaceFolder, exception);
      }
    }
  }

  /** Handle shutdown. */
  public void shutdown() {
    for (final MagikWorkspaceFolder workspaceFolder : this.languageServer.getWorkspaceFolders()) {
      try {
        workspaceFolder.onShutdown();
      } catch (final IOException exception) {
        LOGGER.error(
            "Caught error when shutting down workspacefolder: " + workspaceFolder, exception);
      }
    }
  }
}
