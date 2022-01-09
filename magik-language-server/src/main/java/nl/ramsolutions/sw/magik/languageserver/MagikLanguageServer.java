package nl.ramsolutions.sw.magik.languageserver;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Magik LanguageServer.
 */
public class MagikLanguageServer implements LanguageServer, LanguageClientAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikLanguageServer.class);

    private final ITypeKeeper typeKeeper;
    private final MagikTextDocumentService magikTextDocumentService;
    private final MagikWorkspaceService magikWorkspaceService;
    private LanguageClient languageClient;
    private final List<WorkspaceFolder> workspaceFolders = new ArrayList<>();
    private MagikSettings settings;

    /**
     * Constructor.
     */
    public MagikLanguageServer() {
        this.typeKeeper = new TypeKeeper();
        this.magikTextDocumentService = new MagikTextDocumentService(this, this.typeKeeper);
        this.magikWorkspaceService = new MagikWorkspaceService(this, this.typeKeeper);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(final InitializeParams params) {
        LOGGER.trace("initialize");

        final List<WorkspaceFolder> folders = params.getWorkspaceFolders();
        if (folders != null) {
            this.workspaceFolders.addAll(folders);
        }

        // For older clients.
        final String rootUri = params.getRootUri();
        if (rootUri != null
            && this.workspaceFolders.isEmpty()) {
            final WorkspaceFolder rootFolder = new WorkspaceFolder(rootUri);
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

            return new InitializeResult(capabilities);
        });
    }

    @Override
    public void initialized(final InitializedParams params) {
        LOGGER.trace("initialized");
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        LOGGER.trace("shutdown");
        return CompletableFuture.supplyAsync(() -> null);
    }

    @Override
    public void exit() {
        // Nothing to do.
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
    public void connect(final LanguageClient newLanguageClient) {
        this.languageClient = newLanguageClient;
    }

    /**
     * Get the {{LanguageClient}}.
     * @return Language client.
     */
    public LanguageClient getLanguageClient() {
        return this.languageClient;
    }

    /**
     * Get the {{WorkspaceFolder}}s.
     * @return {{WorkspaceFolder}}s.
     */
    public List<WorkspaceFolder> getWorkspaceFolders() {
        return Collections.unmodifiableList(this.workspaceFolders);
    }

    /**
     * Set settings for workspace.
     * @param settings Settings object.
     */
    public void setSettings(final JsonObject settings) {
        this.settings = new MagikSettings(settings);
    }

    /**
     * Get settings from workspace.
     * @return Settings object.
     */
    public MagikSettings getMagikSettings() {
        return this.settings;
    }

}
