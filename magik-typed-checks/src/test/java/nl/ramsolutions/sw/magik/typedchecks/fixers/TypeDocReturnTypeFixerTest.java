package nl.ramsolutions.sw.magik.typedchecks.fixers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.junit.jupiter.api.Test;

/** Tests for {@link TypeDocReturnTypeFixer}. */
@SuppressWarnings("checkstyle:MagicNumber")
class TypeDocReturnTypeFixerTest {

  private static final URI DEFAULT_URI = URI.create("memory://source.magik");
  private static final String NEWLINE = System.lineSeparator();

  private List<CodeAction> getCodeActions(final String code, final Range range) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = new MagikTypedFile(DEFAULT_URI, code, definitionKeeper);
    final TypeDocReturnTypeFixer fixer = new TypeDocReturnTypeFixer();
    return fixer.provideCodeActions(magikFile, range);
  }

  @Test
  void testAddReturn() {
    final String code =
        """
        _method obj.method()
          _return 1
        _endmethod
        """;
    final Range range = new Range(new Position(0, 0), new Position(3, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions).hasSize(1);
    final CodeAction codeAction = codeactions.get(0);
    assertThat(codeAction)
        .isEqualTo(
            new CodeAction(
                "Add @return type sw:integer",
                new TextEdit(
                    new Range(new Position(2, 0), new Position(2, 0)),
                    "\t## @return {sw:integer} Description" + NEWLINE)));
  }

  @Test
  void testRemoveReturn() {
    final String code =
        """
        _method obj.method()
          ## @return {sw:integer} Test
        _endmethod
        """;
    final Range range = new Range(new Position(0, 0), new Position(3, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions).hasSize(1);
    final CodeAction codeAction = codeactions.get(0);
    assertThat(codeAction)
        .isEqualTo(
            new CodeAction(
                "Remove @return type",
                new TextEdit(new Range(new Position(2, 0), new Position(3, 0)), "")));
  }

  @Test
  void testUpdateReturn() {
    final String code =
        """
        _method obj.method()
          ## @return {sw:float} Test
          _return 1
        _endmethod
        """;
    final Range range = new Range(new Position(0, 0), new Position(4, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions).hasSize(1);
    final CodeAction codeAction = codeactions.get(0);
    assertThat(codeAction)
        .isEqualTo(
            new CodeAction(
                "Update @return type to sw:integer",
                new TextEdit(new Range(new Position(2, 14), new Position(2, 22)), "sw:integer")));
  }

  @Test
  void testNoChangeReturn() {
    final String code =
        """
        _method obj.method(param1, param2)
          ## @return {sw:integer}
          _return 1
        _endmethod
        """;
    final Range range = new Range(new Position(0, 0), new Position(4, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions).isEmpty();
  }
}
