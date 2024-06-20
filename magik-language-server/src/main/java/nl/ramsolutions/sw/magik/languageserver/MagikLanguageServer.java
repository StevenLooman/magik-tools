package nl.ramsolutions.sw.magik.languageserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import nl.ramsolutions.sw.MagikToolsProperties;
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

/** Magik LanguageServer. */
public class MagikLanguageServer implements LanguageServer, LanguageClientAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikLanguageServer.class);

  private final MagikToolsProperties languageServerProperties;
  private final IDefinitionKeeper definitionKeeper;
  private final List<MagikWorkspaceFolder> workspaceFolders = new ArrayList<>();
  private final MagikTextDocumentService magikTextDocumentService;
  private final MagikWorkspaceService magikWorkspaceService;
  private final MagikNotebookDocumentService magikNotebookDocumentService;
  private LanguageClient languageClient;

  /**
   * Constructor.
   *
   * @throws IOException -
   */
  public MagikLanguageServer() throws IOException {
    this.languageServerProperties = new MagikToolsProperties();
    // We assume the DefinitionKeeper gets its types from a types database (.jsonl file).
    this.definitionKeeper = new DefinitionKeeper(false);
    this.magikTextDocumentService =
        new MagikTextDocumentService(this, this.languageServerProperties, this.definitionKeeper);
    this.magikWorkspaceService =
        new MagikWorkspaceService(this, this.languageServerProperties, this.definitionKeeper);
    this.magikNotebookDocumentService = new MagikNotebookDocumentService(this);
  }

  @SuppressWarnings("deprecation")
  @Override
  public CompletableFuture<InitializeResult> initialize(final InitializeParams params) {
    LOGGER.trace("initialize");

    // Log PID and version.
    final ProcessHandle processHandle = ProcessHandle.current();
    LOGGER.info("PID: {}", processHandle.pid());
    final String version = this.getClass().getPackage().getImplementationVersion();
    LOGGER.info("Version: {}", version);

    final String rootUri = params.getRootUri();
    if (params.getWorkspaceFolders() != null && !params.getWorkspaceFolders().isEmpty()) {
      params.getWorkspaceFolders().stream()
          .map(
              workspaceFolder ->
                  new MagikWorkspaceFolder(
                      workspaceFolder, this.definitionKeeper, this.languageServerProperties))
          .forEach(this.workspaceFolders::add);
    } else if (rootUri != null) {
      final WorkspaceFolder rootFolder = new WorkspaceFolder(rootUri, "workspace");
      final MagikWorkspaceFolder rootWorkspaceFolder =
          new MagikWorkspaceFolder(
              rootFolder, this.definitionKeeper, this.languageServerProperties);
      this.workspaceFolders.add(rootWorkspaceFolder);
    }

    // Report workspace folders.
    if (this.workspaceFolders.isEmpty()) {
      LOGGER.debug("No workspace folders!");
    } else {
      this.workspaceFolders.forEach(
          workspaceFolder -> LOGGER.debug("Workspace folder: {}", workspaceFolder));
    }

    return CompletableFuture.supplyAsync(
        () -> {
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
    LOGGER.trace("trace, value: {}", params.getValue());
    // Do nothing.
  }

  @Override
  public void initialized(final InitializedParams params) {
    LOGGER.trace("initialized");
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    LOGGER.trace("shutdown");

    return CompletableFuture.supplyAsync(
        () -> {
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
   *
   * @return Language client.
   */
  public LanguageClient getLanguageClient() {
    return this.languageClient;
  }

  /**
   * Get the {@link MagikWorkspaceFolder}s.
   *
   * @return {@link MagikWorkspaceFolder}s.
   */
  public List<MagikWorkspaceFolder> getWorkspaceFolders() {
    return Collections.unmodifiableList(this.workspaceFolders);
  }
}
