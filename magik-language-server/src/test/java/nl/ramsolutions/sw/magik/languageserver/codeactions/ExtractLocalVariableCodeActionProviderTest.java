package nl.ramsolutions.sw.magik.languageserver.codeactions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.formatting.MagikFormattingSettings;
import org.junit.jupiter.api.Test;

/** Test ExtractLocalVariableCodeActionProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class ExtractLocalVariableCodeActionProviderTest {

  /** Formatting properties using 2-space indentation. */
  private static final MagikToolsProperties SPACE_PROPERTIES =
      new MagikToolsProperties(
          Map.of(
              MagikFormattingSettings.KEY_MAGIK_FORMATTING_INDENT_CHAR, "space",
              MagikFormattingSettings.KEY_MAGIK_FORMATTING_INDENT_WIDTH, "2"));

  private List<CodeAction> provideCodeActions(final String code, final Range range) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(
            SPACE_PROPERTIES, MagikTypedFile.DEFAULT_URI, code, new DefinitionKeeper());
    final ExtractLocalVariableCodeActionProvider provider =
        new ExtractLocalVariableCodeActionProvider();
    return provider.provideCodeActions(magikFile, range);
  }

  // --- Positive tests ---

  @Test
  void testExtractLiteralToLocalVariable() {
    // Select just the literal `2` on line 2.
    // Line 2:  "    write(2)"
    // Columns:  0123456789 01
    //                     ^^ col 10-11
    final String code =
        """
        _method my_object.test_method()
            write(2)
        _endmethod
        $
        """;
    final Range range = new Range(new Position(2, 10), new Position(2, 11));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(1);

    // Replace `2` with `extracted_variable`; insert `_local extracted_variable << 2\n    ` before
    // the statement.
    final TextEdit replaceEdit =
        new TextEdit(new Range(new Position(2, 10), new Position(2, 11)), "extracted_variable");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(2, 4), new Position(2, 4)),
            "_local extracted_variable << 2\n    ");
    // renameLineNumber = 2 + 1 = 3, renameColumn = 10 + 1 = 11
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted variable",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 3, 11));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to local variable",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  @Test
  void testExtractBinaryExpressionToLocalVariable() {
    // Select `a + 2` on line 3.
    // Line 3:  "    write(a + 2)"
    // Columns:  0123456789 01234 5
    //                     ^^^^^ col 10-15
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
            write(a + 2)
        _endmethod
        $
        """;
    final Range range = new Range(new Position(3, 10), new Position(3, 15));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(1);

    final TextEdit replaceEdit =
        new TextEdit(new Range(new Position(3, 10), new Position(3, 15)), "extracted_variable");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(3, 4), new Position(3, 4)),
            "_local extracted_variable << a + 2\n    ");
    // renameLineNumber = 3 + 1 = 4, renameColumn = 10 + 1 = 11
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted variable",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 4, 11));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to local variable",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  @Test
  void testExtractAssignmentRhsToLocalVariable() {
    // Select `a + 2` on line 2 (the RHS of an assignment).
    // Line 2:  "    _local b << a + 2"
    // Columns:  0123456789012345 6789012
    //                            ^^^^^ col 16-21
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
            _local b << a + 2
            write(b)
        _endmethod
        $
        """;
    final Range range = new Range(new Position(3, 16), new Position(3, 21));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(1);

    final TextEdit replaceEdit =
        new TextEdit(new Range(new Position(3, 16), new Position(3, 21)), "extracted_variable");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(3, 4), new Position(3, 4)),
            "_local extracted_variable << a + 2\n    ");
    // renameLineNumber = 3 + 1 = 4, renameColumn = 16 + 1 = 17
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted variable",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 4, 17));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to local variable",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  // --- Rejection tests ---

  @Test
  void testRejectLeaveExpression() {
    // Selecting `_leave` inside a loop must be rejected.
    final String code =
        """
        _method my_object.test_method()
            _loop
                _leave
            _endloop
        _endmethod
        $
        """;
    final Range range = new Range(new Position(3, 8), new Position(3, 14));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).isEmpty();
  }

  @Test
  void testRejectContinueExpression() {
    // Selecting `_continue` inside a loop must be rejected.
    final String code =
        """
        _method my_object.test_method()
            _loop
                _continue
            _endloop
        _endmethod
        $
        """;
    final Range range = new Range(new Position(3, 8), new Position(3, 17));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).isEmpty();
  }

  @Test
  void testRejectOutsideMethod() {
    // Expression not inside any method or proc definition.
    final String code =
        """
        write("hello")
        $
        """;
    final Range range = new Range(new Position(1, 6), new Position(1, 13));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).isEmpty();
  }

  @Test
  void testRejectNonExpression() {
    // Selecting the entire keyword token `_local` is not an extractable expression.
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
        _endmethod
        $
        """;
    final Range range = new Range(new Position(2, 4), new Position(2, 10));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).isEmpty();
  }
}
