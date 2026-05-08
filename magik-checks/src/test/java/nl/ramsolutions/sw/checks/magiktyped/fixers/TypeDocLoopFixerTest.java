package nl.ramsolutions.sw.checks.magiktyped.fixers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.junit.jupiter.api.Test;

/** Tests for {@link TypeDocLoopFixer}. */
@SuppressWarnings("checkstyle:MagicNumber")
class TypeDocLoopFixerTest {

  private static final String NEWLINE = System.lineSeparator();

  private List<CodeAction> getCodeActions(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final TypeDocLoopFixer fixer = new TypeDocLoopFixer();
    final Range range = new Range(new Position(0, 0), new Position(Integer.MAX_VALUE, 0));
    return fixer.provideCodeActions(magikFile, range);
  }

  @Test
  void testAddLoop() {
    final String code =
        """
        _iter _method obj.method()
          _loopbody(1)
        _endmethod
        """;
    final List<CodeAction> codeActions = this.getCodeActions(code);
    assertThat(codeActions)
        .containsOnly(
            new CodeAction(
                "Add @loop type sw:integer",
                new TextEdit(
                    new Range(new Position(2, 0), new Position(2, 0)),
                    "\t## @loop {sw:integer} Description" + NEWLINE)));
  }

  @Test
  void testRemoveLoop() {
    final String code =
        """
        _method obj.method()
          ## @loop {sw:integer} Test
        _endmethod
        """;
    final List<CodeAction> codeActions = this.getCodeActions(code);
    assertThat(codeActions)
        .containsOnly(
            new CodeAction(
                "Remove @loop type",
                new TextEdit(new Range(new Position(2, 0), new Position(3, 0)), "")));
  }

  @Test
  void testUpdateLoop() {
    final String code =
        """
        _iter _method obj.method()
          ## @loop {sw:float} Test
          _loopbody(1)
        _endmethod
        """;
    final List<CodeAction> codeActions = this.getCodeActions(code);
    assertThat(codeActions)
        .containsOnly(
            new CodeAction(
                "Update @loop type to sw:integer",
                new TextEdit(new Range(new Position(2, 12), new Position(2, 20)), "sw:integer")));
  }

  @Test
  void testNoChangeLoop() {
    final String code =
        """
        _iter _method obj.method()
          ## @loop {sw:integer}
          _loopbody(1)
        _endmethod
        """;
    final List<CodeAction> codeActions = this.getCodeActions(code);
    assertThat(codeActions).isEmpty();
  }
}
