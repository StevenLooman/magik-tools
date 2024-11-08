package nl.ramsolutions.sw.magik.languageserver.rename;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.SmallworldProjectExtension;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MethodRenamerTest {

  @RegisterExtension final SmallworldProjectExtension extension = new SmallworldProjectExtension();

  @Test
  void testPrepareRenameOnDefinition() throws IOException {
    final String code =
        """
        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final Position position = new Position(1, 10); // On `method_name`.

    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);

    final MethodRenamer renamer = new MethodRenamer(magikFile, node);
    final PrepareRenameResult prepareRename = renamer.prepareRename();
    assertThat(prepareRename)
        .isEqualTo(
            new PrepareRenameResult(
                Lsp4jConversion.rangeToLsp4j(new Range(new Position(1, 10), new Position(1, 21))),
                "method_name"));
  }

  @Test
  void testPrepareRenameOnInvocation() throws IOException {
    final String code =
        """
        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final Position position = new Position(2, 10); // On `method_name`.

    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);

    final MethodRenamer renamer = new MethodRenamer(magikFile, node);
    final PrepareRenameResult prepareRename = renamer.prepareRename();
    assertThat(prepareRename)
        .isEqualTo(
            new PrepareRenameResult(
                Lsp4jConversion.rangeToLsp4j(new Range(new Position(2, 8), new Position(2, 19))),
                "method_name"));
  }

  @Test
  void testRenameMethodSelf() throws IOException {
    final String code =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final Position position = new Position(3, 10); // On `method_name`.

    final Path path = this.extension.pathOf("/source.magik");
    final MagikTypedFile magikFile = this.extension.addMagikFile(path, code);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);

    final MethodRenamer renamer = new MethodRenamer(magikFile, node);
    final Map<URI, List<TextEdit>> renames = renamer.provideRename("new_method_name");
    assertThat(renames)
        .isEqualTo(
            Map.of(
                URI.create("memory:test:///source.magik"),
                List.of(
                    new TextEdit(
                        new Range(new Position(3, 10), new Position(3, 21)), "new_method_name"),
                    new TextEdit(
                        new Range(new Position(4, 8), new Position(4, 19)), "new_method_name"))));
  }

  @Test
  void testRenameMethodMultiFile() throws IOException {
    final String codeA =
        """
        def_slotted_exemplar(:a, {})

        _method a.method_name()
          _self.method_name()
        _endmethod
        """;
    final String codeB =
        """
        _method b.method()
          a.method_name()
        _endmethod
        """;
    final Position position = new Position(3, 10); // On `method_name`.

    final Path pathA = this.extension.pathOf("/source_a.magik");
    final MagikTypedFile magikFileA = this.extension.addMagikFile(pathA, codeA);
    final Path pathB = this.extension.pathOf("/source_b.magik");
    this.extension.addMagikFile(pathB, codeB);

    final AstNode topNode = magikFileA.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);

    final MethodRenamer renamer = new MethodRenamer(magikFileA, node);
    final Map<URI, List<TextEdit>> renames = renamer.provideRename("new_method_name");
    assertThat(renames)
        .isEqualTo(
            Map.of(
                URI.create("memory:test:///source_a.magik"),
                List.of(
                    new TextEdit(
                        new Range(new Position(3, 10), new Position(3, 21)), "new_method_name"),
                    new TextEdit(
                        new Range(new Position(4, 8), new Position(4, 19)), "new_method_name")),
                URI.create("memory:test:///source_b.magik"),
                List.of(
                    new TextEdit(
                        new Range(new Position(2, 4), new Position(2, 15)), "new_method_name"))));
  }
}
