package nl.ramsolutions.sw.magik.languageserver.rename;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.SmallworldProjectExtension;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/** Tests for {@link RenameProvider}. */
@SuppressWarnings("checkstyle:MagicNumber")
class RenameProviderTest {

  @RegisterExtension final SmallworldProjectExtension extension = new SmallworldProjectExtension();

  private PrepareRenameResult providePrepareRename(final String code, final Position position) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final RenameProvider provider = new RenameProvider();
    final var result = provider.providePrepareRename(magikFile, position);
    return result == null ? null : result.getSecond();
  }

  private WorkspaceEdit provideRename(
      final String code, final Position position, final String newName) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final RenameProvider provider = new RenameProvider();
    return provider.provideRename(magikFile, position, newName);
  }

  @Test
  void testProvidePrepareRenameVariableLocal() throws IOException {
    final String code =
        """
        _block
            _local var
            show(var)
        _endblock
        """;
    final Position position = new Position(1, 12); // On `var`.

    final PrepareRenameResult prepareRename = this.providePrepareRename(code, position);
    assertThat(prepareRename)
        .isEqualTo(
            new PrepareRenameResult(new Range(new Position(1, 11), new Position(1, 14)), "var"));
  }

  @Test
  void testProvideRenameVariableLocal() throws IOException {
    final String code =
        """
        _block
            _local var
            show(var)
        _endblock
        """;
    final Position position = new Position(1, 12); // On `var`.

    final WorkspaceEdit workspaceEdit = this.provideRename(code, position, "new");
    assertThat(workspaceEdit)
        .isEqualTo(
            new WorkspaceEdit(
                Map.of(
                    MagikTypedFile.DEFAULT_URI.toString(),
                    List.of(
                        new TextEdit(new Range(new Position(1, 11), new Position(1, 14)), "new"),
                        new TextEdit(new Range(new Position(2, 9), new Position(2, 12)), "new")))));
  }

  @Test
  void testProvidePrepareRenameMethodDefinition() throws IOException {
    final String code =
        """
        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final Position position = new Position(0, 10); // On `method_name`.

    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final RenameProvider provider = new RenameProvider();
    final var result = provider.providePrepareRename(magikFile, position);
    final PrepareRenameResult prepareRename = result.getSecond();
    assertThat(prepareRename)
        .isEqualTo(
            new PrepareRenameResult(
                new Range(new Position(0, 10), new Position(0, 21)), "method_name"));
  }

  @Test
  void testProvidePrepareRenameMethodInvocation() throws IOException {
    final String code =
        """
        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final Position position = new Position(1, 10); // On `method_name`.

    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final RenameProvider provider = new RenameProvider();
    final var result = provider.providePrepareRename(magikFile, position);
    final PrepareRenameResult prepareRename = result.getSecond();
    assertThat(prepareRename)
        .isEqualTo(
            new PrepareRenameResult(
                new Range(new Position(1, 8), new Position(1, 19)), "method_name"));
  }

  @Test
  void testProvideRenameMethod() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final Position position = new Position(2, 10); // On `method_name`.

    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final RenameProvider provider = new RenameProvider();
    final WorkspaceEdit workspaceEdit = provider.provideRename(magikFile, position, "new_name");
    assertThat(workspaceEdit)
        .isEqualTo(
            new WorkspaceEdit(
                Map.of(
                    "memory:test:///source.magik",
                    List.of(
                        new TextEdit(
                            new Range(new Position(2, 10), new Position(2, 21)), "new_name"),
                        new TextEdit(
                            new Range(new Position(3, 8), new Position(3, 19)), "new_name")))));
  }

  @Test
  void testProvidePrepareRenameNoRenamerReturnsNull() throws IOException {
    final String code =
        """
        _global my_global << 1

        _method object.m()
            _return my_global
        _endmethod
        """;
    final Position position = new Position(3, 15); // On `my_global`.

    final PrepareRenameResult prepareRename = this.providePrepareRename(code, position);
    assertThat(prepareRename).isNull();
  }

  @Test
  void testProvideRenameNoRenamerReturnsNull() throws IOException {
    final String code =
        """
        _global my_global << 1

        _method object.m()
            _return my_global
        _endmethod
        """;
    final Position position = new Position(3, 15); // On `my_global`.

    final WorkspaceEdit workspaceEdit = this.provideRename(code, position, "new_name");
    assertThat(workspaceEdit).isNull();
  }
}
