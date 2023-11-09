package nl.ramsolutions.sw.magik.languageserver;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.magik.analysis.typing.ClassInfoTypeKeeperReader;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.ReadOnlyTypeKeeperAdapter;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.indexer.MagikIndexer;
import nl.ramsolutions.sw.magik.analysis.typing.io.JsonTypeKeeperReader;
import nl.ramsolutions.sw.magik.languageserver.munit.MUnitTestItem;
import nl.ramsolutions.sw.magik.languageserver.munit.MUnitTestItemProvider;
import nl.ramsolutions.sw.magik.languageserver.symbol.SymbolProvider;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.ServerCapabilities;
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

/**
 * Magik WorkspaceService.
 */
public class MagikWorkspaceService implements WorkspaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikWorkspaceService.class);

    private final MagikLanguageServer languageServer;
    private final ITypeKeeper typeKeeper;
    private final IgnoreHandler ignoreHandler;
    private final MagikIndexer magikIndexer;
    private final SymbolProvider symbolProvider;
    private final MUnitTestItemProvider testItemProvider;

    /**
     * Constructor.
     * @param languageServer Owner language server.
     * @param typeKeeper {@link TypeKeeper} used for type storage.
     */
    public MagikWorkspaceService(final MagikLanguageServer languageServer, final ITypeKeeper typeKeeper) {
        this.languageServer = languageServer;
        this.typeKeeper = typeKeeper;

        this.ignoreHandler = new IgnoreHandler();
        this.magikIndexer = new MagikIndexer(typeKeeper);
        final ITypeKeeper roTypeKeeper = new ReadOnlyTypeKeeperAdapter(typeKeeper);
        this.symbolProvider = new SymbolProvider(roTypeKeeper);
        this.testItemProvider = new MUnitTestItemProvider(roTypeKeeper);
    }

    /**
     * Set capabilities.
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

        LOGGER.debug("New settings: {}", settings);
        MagikSettings.INSTANCE.setSettings(settings);

        this.runIndexersInBackground();
    }

    private void runIgnoreFilesIndexer() {
        LOGGER.trace("Running IgnoreHandler indexer");
        for (final WorkspaceFolder workspaceFolder : this.languageServer.getWorkspaceFolders()) {
            final String uriStr = workspaceFolder.getUri();
            final URI uri = URI.create(uriStr);
            final Path workspacePath = Path.of(uri);

            try (Stream<Path> stream = Files.walk(workspacePath)) {
                stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(".magik-tools-ignore"))
                    .forEach(path -> {
                        try {
                            this.ignoreHandler.addIgnoreFile(path);
                        } catch (final IOException exception) {
                            LOGGER.error(exception.getMessage(), exception);
                        }
                    });
            } catch (final IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        }
    }

    private void runIndexer() {
        LOGGER.trace("Running MagikIndexer");
        for (final WorkspaceFolder workspaceFolder : this.languageServer.getWorkspaceFolders()) {
            try {
                LOGGER.debug("Running MagikIndexer from: {}", workspaceFolder.getUri());
                final Stream<Path> indexableFiles = this.getIndexableFiles(workspaceFolder);
                this.magikIndexer.indexPaths(indexableFiles);
            } catch (final IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        }
    }

    private void readLibsClassInfos(final List<String> libsDirs) {
        LOGGER.trace("Reading libs docs from: {}", libsDirs);

        libsDirs.forEach(pathStr -> {
            final Path path = Path.of(pathStr);
            if (!Files.exists(path)) {
                LOGGER.warn("Path to libs dir does not exist: {}", pathStr);
                return;
            }

            try {
                ClassInfoTypeKeeperReader.readLibsDirectory(path, this.typeKeeper);
            } catch (final IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        });
    }

    /**
     * Read the type databases from the given path.
     * @param typeDbPaths Paths to type databases.
     */
    public void readTypesDbs(final List<String> typeDbPaths) {
        LOGGER.trace("Reading type databases from: {}", typeDbPaths);

        typeDbPaths.forEach(pathStr -> {
            final Path path = Path.of(pathStr);
            if (!Files.exists(path)) {
                LOGGER.warn("Path to types database does not exist: {}", pathStr);
                return;
            }

            try {
                JsonTypeKeeperReader.read(path, this.typeKeeper);
            } catch (final IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public void didChangeWatchedFiles(final DidChangeWatchedFilesParams params) {
        params.getChanges().stream()
            .forEach(fileEvent -> {
                final URI uri = URI.create(fileEvent.getUri());
                final Path path = Path.of(uri);
                final FileSystem fileSystem = path.getFileSystem();

                if (fileSystem.getPathMatcher("glob:**/*.magik").matches(path)) {
                    this.handleMagikFileChange(fileEvent);
                } else if (fileSystem.getPathMatcher("glob:**/.magik-tools-ignore").matches(path)) {
                    this.handleIgnoreFileChange(fileEvent);
                }
            });
    }

    private void handleMagikFileChange(final FileEvent fileEvent) {
        final URI uri = URI.create(fileEvent.getUri());
        final Path path = Path.of(uri);

        // Don't index if ignored.
        if (this.ignoreHandler.isIgnored(path)) {
            return;
        }

        if (fileEvent.getType() == FileChangeType.Created) {
            this.magikIndexer.indexPathCreated(path);
        } else if (fileEvent.getType() == FileChangeType.Changed) {
            this.magikIndexer.indexPathChanged(path);
        } else if (fileEvent.getType() == FileChangeType.Deleted) {
            this.magikIndexer.indexPathDeleted(path);
        }
    }

    private void handleIgnoreFileChange(final FileEvent fileEvent) {
        final URI uri = URI.create(fileEvent.getUri());
        final Path path = Path.of(uri);
        try {
            if (fileEvent.getType() == FileChangeType.Created
                || fileEvent.getType() == FileChangeType.Changed) {
                this.ignoreHandler.addIgnoreFile(path);
            } else if (fileEvent.getType() == FileChangeType.Deleted) {
                this.ignoreHandler.removeIgnoreFile(path);
            }
        } catch (final IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
    }

    @Override
    public CompletableFuture<
            Either<List<? extends org.eclipse.lsp4j.SymbolInformation>, List<? extends WorkspaceSymbol>>
            > symbol(WorkspaceSymbolParams params) {
        final String query = params.getQuery();
        LOGGER.trace("symbol, query: {}", query);

        return CompletableFuture.supplyAsync(() -> {
            final List<WorkspaceSymbol> queryResults = this.symbolProvider.getSymbols(query);
            LOGGER.debug("Symbols found for: '{}', count: {}", query, queryResults.size());
            return Either.forRight(queryResults);
        });
    }

    // region: Additional commands.
    /**
     * Re-index all magik files.
     * @return CompletableFuture.
     */
    @JsonRequest(value = "custom/reIndex")
    public CompletableFuture<Void> reIndex() {
        return CompletableFuture.runAsync(() -> {
            this.typeKeeper.clear();

            this.runIndexersInBackground();
        });
    }

    /**
     * Get test items.
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

    private Stream<Path> getIndexableFiles(final WorkspaceFolder workspaceFolder) throws IOException {
        final String uriStr = workspaceFolder.getUri();
        final URI uri = URI.create(uriStr);
        final Path workspacePath = Path.of(uri);

        return Files.walk(workspacePath)
                .filter(path -> !this.ignoreHandler.isIgnored(path));
    }

    private void runIndexers() {
        LOGGER.trace("Run indexers");

        // Read types db.
        final List<String> typesDbPaths = MagikSettings.INSTANCE.getTypingTypeDatabasePaths();
        this.readTypesDbs(typesDbPaths);

        // Read class_infos from libs/ dirs.
        final List<String> libsDirs = MagikSettings.INSTANCE.getLibsDirs();
        this.readLibsClassInfos(libsDirs);

        // Index .magik-tools-ignore files.
        this.runIgnoreFilesIndexer();

        // Run indexer.
        this.runIndexer();
    }

    @SuppressWarnings("IllegalCatch")
    private void runIndexersInBackground() {
        LOGGER.trace("Run background indexer");

        final LanguageClient languageClient = this.languageServer.getLanguageClient();
        final WorkDoneProgressCreateParams params = new WorkDoneProgressCreateParams();
        final String token = UUID.randomUUID().toString();
        params.setToken(token);
        languageClient.createProgress(params);

        CompletableFuture.runAsync(() -> {
            LOGGER.trace("Start indexing in background");
            final ProgressParams progressParams = new ProgressParams();
            progressParams.setToken(token);

            final WorkDoneProgressBegin begin = new WorkDoneProgressBegin();
            begin.setTitle("Indexing Magik sources");
            progressParams.setValue(Either.forLeft(begin));
            languageClient.notifyProgress(progressParams);

            try {
                this.runIndexers();
            } catch (final Exception exception) {
                LOGGER.error(exception.getMessage(), exception);
            }

            final WorkDoneProgressEnd end = new WorkDoneProgressEnd();
            end.setMessage("Done indexing Magik sources");
            progressParams.setValue(Either.forLeft(end));
            languageClient.notifyProgress(progressParams);
            LOGGER.trace("Done indexing Magik sources in background");
        });
    }

    public void shutdown() {
        // TODO: Dump type database, and read it again when starting?
        //       Requires timestamping of definitions/files!
    }

}
