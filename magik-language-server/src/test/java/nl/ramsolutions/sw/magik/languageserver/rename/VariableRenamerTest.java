package nl.ramsolutions.sw.magik.languageserver.rename;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.junit.jupiter.api.Test;

/** Tests for {@link VariableRenamer}. */
public class VariableRenamerTest {

  private VariableRenamer getRenamer(final String code, final Position position) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode node = AstQuery.nodeAt(topNode, position, MagikGrammar.IDENTIFIER);
    return new VariableRenamer(magikFile, node);
  }

  @Test
  void testPrepareRenameVariableLocal() throws IOException {
    final String code =
        """
        _block
            _local var
            show(var)
        _endblock
        """;
    final Position position = new Position(2, 12); // On `var`.

    final VariableRenamer renamer = this.getRenamer(code, position);
    final PrepareRenameResult prepareRename = renamer.prepareRename();
    assertThat(prepareRename)
        .isEqualTo(
            new PrepareRenameResult(
                Lsp4jConversion.rangeToLsp4j(new Range(new Position(2, 11), new Position(2, 14))),
                "var"));
  }

  @Test
  void testRenameVariableLocal() throws IOException {
    final String code =
        """
        _block
            _local var
            show(var)
        _endblock
        """;
    final Position position = new Position(2, 12); // On `var`.

    final VariableRenamer renamer = this.getRenamer(code, position);
    final Map<URI, List<TextEdit>> renames = renamer.provideRename("new");
    assertThat(renames)
        .isEqualTo(
            Map.of(
                URI.create("memory:///source.magik"),
                List.of(
                    new TextEdit(new Range(new Position(2, 11), new Position(2, 14)), "new"),
                    new TextEdit(new Range(new Position(3, 9), new Position(3, 12)), "new"))));
  }

  @Test
  void testRenameVariableLocalFromUsage() throws IOException {
    final String code =
        """
        _block
            _local var
            show(var)
        _endblock
        """;
    final Position position = new Position(3, 10); // On `var`.

    final VariableRenamer renamer = this.getRenamer(code, position);
    final Map<URI, List<TextEdit>> renames = renamer.provideRename("new");
    assertThat(renames)
        .isEqualTo(
            Map.of(
                URI.create("memory:///source.magik"),
                List.of(
                    new TextEdit(new Range(new Position(2, 11), new Position(2, 14)), "new"),
                    new TextEdit(new Range(new Position(3, 9), new Position(3, 12)), "new"))));
  }

  @Test
  void testPrepareRenameVariableForVariable() throws IOException {
    final String code =
        """
        _block
            _for iter_var _over 1.upto(10)
            _loop
                show(iter_var)
            _endloop
        _endblock
        """;
    final Position position = new Position(2, 10); // on `iter_var`.

    final VariableRenamer renamer = this.getRenamer(code, position);
    final PrepareRenameResult prepareRename = renamer.prepareRename();
    assertThat(prepareRename)
        .isEqualTo(
            new PrepareRenameResult(
                Lsp4jConversion.rangeToLsp4j(new Range(new Position(2, 9), new Position(2, 17))),
                "iter_var"));
  }

  @Test
  void testRenameVariableForVariable() throws IOException {
    final String code =
        """
        _block
            _for iter_var _over 1.upto(10)
            _loop
                show(iter_var)
            _endloop
        _endblock
        """;
    final Position position = new Position(2, 12); // On `iter_var`.

    final VariableRenamer renamer = this.getRenamer(code, position);
    final Map<URI, List<TextEdit>> renames = renamer.provideRename("new");
    assertThat(renames)
        .isEqualTo(
            Map.of(
                URI.create("memory:///source.magik"),
                List.of(
                    new TextEdit(new Range(new Position(2, 9), new Position(2, 17)), "new"),
                    new TextEdit(new Range(new Position(4, 13), new Position(4, 21)), "new"))));
  }

  @Test
  void testRenameVariableOptionalParameter() throws IOException {
    final String code =
        """
        _method a.b(_optional param1)
            write(param1)
        _endmethod
        """;
    final Position position = new Position(2, 12); // On `param1`.

    final VariableRenamer renamer = this.getRenamer(code, position);
    final Map<URI, List<TextEdit>> renames = renamer.provideRename("new");
    assertThat(renames)
        .isEqualTo(
            Map.of(
                URI.create("memory:///source.magik"),
                List.of(
                    new TextEdit(new Range(new Position(1, 22), new Position(1, 28)), "new"),
                    new TextEdit(new Range(new Position(2, 10), new Position(2, 16)), "new"))));
  }
}
