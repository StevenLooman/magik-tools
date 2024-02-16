package nl.ramsolutions.sw.magik.languageserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisConfiguration;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SetTraceParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.NotebookDocumentService;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Magik LanguageServer.
 */
public class MagikLanguageServer implements LanguageServer, LanguageClientAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikLanguageServer.class);

    private final MagikAnalysisConfiguration analysisConfiguration;
    private final IDefinitionKeeper definitionKeeper;
    private final List<WorkspaceFolder> workspaceFolders = new ArrayList<>();
    private final MagikTextDocumentService magikTextDocumentService;
    private final MagikWorkspaceService magikWorkspaceService;
    private final MagikNotebookDocumentService magikNotebookDocumentService;
    private LanguageClient languageClient;

    /**
     * Constructor.
     * @throws IOException
     */
    public MagikLanguageServer() throws IOException {
        // We assume the DefinitionKeeper gets its types from a types database (.jsonl file).
        this.analysisConfiguration = new MagikAnalysisConfiguration();
        this.definitionKeeper = new DefinitionKeeper(false);
        this.magikTextDocumentService =
            new MagikTextDocumentService(this, this.analysisConfiguration, this.definitionKeeper);
        this.magikWorkspaceService = new MagikWorkspaceService(this, this.analysisConfiguration, this.definitionKeeper);
        this.magikNotebookDocumentService = new MagikNotebookDocumentService(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public CompletableFuture<InitializeResult> initialize(final InitializeParams params) {
        LOGGER.trace("initialize");

        // Log PID.
        final ProcessHandle processHandle = ProcessHandle.current();
        LOGGER.info("PID: {}", processHandle.pid());

        // Log server version.
        final String version = this.getClass().getPackage().getImplementationVersion();
        LOGGER.info("Version: {}", version);

        final List<WorkspaceFolder> folders = params.getWorkspaceFolders();
        if (folders != null) {
            this.workspaceFolders.addAll(folders);
        }

        // For older clients.
        final String rootUri = params.getRootUri();
        if (rootUri != null
            && this.workspaceFolders.isEmpty()) {
            final WorkspaceFolder rootFolder = new WorkspaceFolder(rootUri, "workspace");
            this.workspaceFolders.add(rootFolder);
        }

        // Report workspace folders.
        if (this.workspaceFolders.isEmpty()) {
            LOGGER.debug("No workspace folders!");
        } else {
            this.workspaceFolders.forEach(workspaceFolder -> LOGGER.debug("Workspace folder: {}", workspaceFolder));
        }

        return CompletableFuture.supplyAsync(() -> {
            // Set capabilities.
            final ServerCapabilities capabilities = new ServerCapabilities();
            this.magikTextDocumentService.setCapabilities(capabilities);
            this.magikWorkspaceService.setCapabilities(capabilities);
            this.magikNotebookDocumentService.setCapabilities(capabilities);

            return new InitializeResult(capabilities);
        });
    }

    @Override
    public void setTrace(final SetTraceParams params) {
        LOGGER.trace(
            "trace, value: {}",
            params.getValue());
        // Do nothing.
    }

    @Override
    public void initialized(final InitializedParams params) {
        LOGGER.trace("initialized");
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        LOGGER.trace("shutdown");

        return CompletableFuture.supplyAsync(() -> {
            this.magikWorkspaceService.shutdown();

            return null;
        });
    }

    @Override
    public void exit() {
        LOGGER.trace("exit");

        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return this.magikTextDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return this.magikWorkspaceService;
    }

    @Override
    public NotebookDocumentService getNotebookDocumentService() {
        return this.magikNotebookDocumentService;
    }

    @Override
    public void connect(final LanguageClient newLanguageClient) {
        this.languageClient = newLanguageClient;
    }

    /**
     * Get the {@link LanguageClient}.
     * @return Language client.
     */
    public LanguageClient getLanguageClient() {
        return this.languageClient;
    }

    /**
     * Get the {@link WorkspaceFolder}s.
     * @return {@link WorkspaceFolder}s.
     */
    public List<WorkspaceFolder> getWorkspaceFolders() {
        return Collections.unmodifiableList(this.workspaceFolders);
    }

}
