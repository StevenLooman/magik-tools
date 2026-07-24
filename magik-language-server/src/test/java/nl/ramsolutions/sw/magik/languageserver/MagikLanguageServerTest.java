package nl.ramsolutions.sw.magik.languageserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link MagikLanguageServer}. */
class MagikLanguageServerTest {

  private MagikLanguageServer initializedServer(final Path workspacePath) throws Exception {
    final MagikLanguageServer server = new MagikLanguageServer();
    final InitializeParams params = new InitializeParams();
    params.setCapabilities(new ClientCapabilities());
    params.setWorkspaceFolders(
        List.of(new WorkspaceFolder(workspacePath.toUri().toString(), "test")));
    server.initialize(params).get();
    return server;
  }

  @Test
  void testPersistWritesTypesDatabase(@TempDir final Path workspacePath) throws Exception {
    final MagikLanguageServer server = this.initializedServer(workspacePath);

    server.shutdownWorkspaces();

    assertThat(Files.exists(workspacePath.resolve("types.jsonl"))).isTrue();
  }

  @Test
  void testPersistIsIdempotent(@TempDir final Path workspacePath) throws Exception {
    final MagikLanguageServer server = this.initializedServer(workspacePath);
    final Path typesDbPath = workspacePath.resolve("types.jsonl");

    server.shutdownWorkspaces();
    assertThat(Files.exists(typesDbPath)).isTrue();

    // A second persist (e.g. the shutdown hook firing after a clean LSP shutdown) must be a no-op,
    // so removing the database and persisting again leaves it removed.
    Files.delete(typesDbPath);
    server.shutdownWorkspaces();
    assertThat(Files.exists(typesDbPath)).isFalse();
  }
}
