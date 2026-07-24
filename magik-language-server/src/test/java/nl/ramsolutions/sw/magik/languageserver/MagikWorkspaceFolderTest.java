package nl.ramsolutions.sw.magik.languageserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.io.JsonDefinitionReader;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link MagikWorkspaceFolder}. */
class MagikWorkspaceFolderTest {

  @Test
  void testOnShutdownWritesReadableTypesDatabase(@TempDir final Path workspacePath)
      throws IOException {
    // Given a workspace folder with an indexed definition located inside it.
    final URI workspaceUri = workspacePath.toUri();
    final WorkspaceFolder workspaceFolder = new WorkspaceFolder(workspaceUri.toString(), "test");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeRef = TypeString.ofIdentifier("user:my_exemplar", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            new Location(workspacePath.resolve("a.magik").toUri()),
            Instant.now(),
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            typeRef,
            null));
    final MagikWorkspaceFolder magikWorkspaceFolder =
        new MagikWorkspaceFolder(workspaceFolder, definitionKeeper, new MagikToolsProperties());

    // When the workspace folder is shut down.
    magikWorkspaceFolder.onShutdown();

    // Then a types database is written that reads back the definition.
    final Path typesDbPath = workspacePath.resolve("types.jsonl");
    assertThat(Files.exists(typesDbPath)).isTrue();
    final IDefinitionKeeper readBack = new DefinitionKeeper(false);
    JsonDefinitionReader.readTypes(typesDbPath, readBack);
    assertThat(readBack.getExemplarDefinitions(typeRef)).isNotEmpty();
  }

  @Test
  void testOnShutdownReplacesExistingDatabaseWithoutIntermediateLoss(
      @TempDir final Path workspacePath) throws IOException {
    // Given a pre-existing types database.
    final Path typesDbPath = workspacePath.resolve("types.jsonl");
    Files.writeString(typesDbPath, "stale content\n");

    final URI workspaceUri = workspacePath.toUri();
    final WorkspaceFolder workspaceFolder = new WorkspaceFolder(workspaceUri.toString(), "test");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeRef = TypeString.ofIdentifier("user:my_exemplar", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            new Location(workspacePath.resolve("a.magik").toUri()),
            Instant.now(),
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            typeRef,
            null));
    final MagikWorkspaceFolder magikWorkspaceFolder =
        new MagikWorkspaceFolder(workspaceFolder, definitionKeeper, new MagikToolsProperties());

    // When shut down.
    magikWorkspaceFolder.onShutdown();

    // Then the database is replaced with the fresh content (the file always exists on disk).
    assertThat(Files.exists(typesDbPath)).isTrue();
    final IDefinitionKeeper readBack = new DefinitionKeeper(false);
    JsonDefinitionReader.readTypes(typesDbPath, readBack);
    assertThat(readBack.getExemplarDefinitions(typeRef)).isNotEmpty();
  }
}
