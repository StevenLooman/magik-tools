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

/** Test ExtractExpressionCodeActionProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class ExtractExpressionCodeActionProviderTest {

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
    final ExtractExpressionCodeActionProvider provider = new ExtractExpressionCodeActionProvider();
    return provider.provideCodeActions(magikFile, range);
  }

  // --- Positive tests ---

  @Test
  void testExtractLiteralExpressionToMethod() {
    // Select just the literal `2` on line 2.
    // Line 2:  "    _local b << 2"
    // Columns:  0123456789012345 6 7
    //                             ^^ col 16–17
    final String code =
        """
        _method my_object.test_method()
            _local b << 2
            write(b)
        _endmethod
        $
        """;
    final Range range = new Range(new Position(2, 16), new Position(2, 17));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(1);

    // No variables defined before and used within `2`; return type = sw:integer (literal).
    final TextEdit replaceEdit =
        new TextEdit(new Range(new Position(2, 16), new Position(2, 17)), "_self.extracted_method");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(6, 0), new Position(6, 0)),
            """

            _private _method my_object.extracted_method
              ## @return {sw:integer}
              _return 2
            _endmethod
            $
            """);
    // callText = "_self.extracted_method", indexOf("extracted_method") = 6
    // renameColumn = 16 + 6 + 1 = 23
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 2, 23));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to method",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  @Test
  void testExtractExpressionWithParamToMethod() {
    // Select `a + 2` on line 3.
    // Line 3:  "    _local b << a + 2"
    // Columns:  0123456789012345 6789012
    //                            ^^^^^^ col 16–21
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

    // a (defined before, used within) → parameter.
    // return type of ADDITIVE_EXPRESSION not resolved without definitions → no @return.
    final TextEdit replaceEdit =
        new TextEdit(
            new Range(new Position(3, 16), new Position(3, 21)), "_self.extracted_method(a)");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(7, 0), new Position(7, 0)),
            """

            _private _method my_object.extracted_method(a)
              ## @param {sw:integer} a
              _return a + 2
            _endmethod
            $
            """);
    // callText = "_self.extracted_method(a)", indexOf("extracted_method") = 6
    // renameColumn = 16 + 6 + 1 = 23
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 3, 23));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to method",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  @Test
  void testExtractExpressionNoParamToMethod() {
    // Select `1 + 2` on line 2 — both operands are literals, no outer variables used.
    // Line 2:  "    _local a << 1 + 2"
    // Columns:  0123456789012345 6789012
    //                            ^^^^^ col 16–21
    final String code =
        """
        _method my_object.test_method()
            _local a << 1 + 2
            write(a)
        _endmethod
        $
        """;
    final Range range = new Range(new Position(2, 16), new Position(2, 21));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(1);

    // No outer variables used — no parameters.
    final TextEdit replaceEdit =
        new TextEdit(new Range(new Position(2, 16), new Position(2, 21)), "_self.extracted_method");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(6, 0), new Position(6, 0)),
            """

            _private _method my_object.extracted_method
              _return 1 + 2
            _endmethod
            $
            """);
    // callText = "_self.extracted_method", no parens
    // renameColumn = 16 + 6 + 1 = 23
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 2, 23));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to method",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  // --- Rejection tests ---

  @Test
  void testRejectLeaveExpression() {
    // Selecting `_leave` inside a loop must be rejected.
    // Line 3:  "        _leave"
    // Columns:  01234567 8 9012 3
    //                    ^^^^^^ col 8–14
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
    // Line 3:  "        _continue"
    // Columns:  01234567 8 901234567
    //                    ^^^^^^^^^ col 8–17
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
    // Select `"hello"` on line 1.
    final Range range = new Range(new Position(1, 6), new Position(1, 13));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).isEmpty();
  }

  @Test
  void testRejectWholeStatementIsNotExpression() {
    // Selecting the entire keyword token `_local` is not an extractable expression.
    // Line 2:  "    _local a << 1"
    //           0123 4 5678
    //                ^^^^^^ col 4–10 = "_local"
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
        _endmethod
        $
        """;
    // Select just the `_local` keyword — not a valid expression.
    final Range range = new Range(new Position(2, 4), new Position(2, 10));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).isEmpty();
  }

  @Test
  void testExtractExpressionToMethodCopiesPragma() {
    // When the source method has a pragma, the extracted method gets a copy with classify_level
    // restricted (or lower) since the extracted method is always _private.
    final String code =
        """
        _pragma(classify_level=restricted, topic={tests})
        _method my_object.test_method()
            _local a << 1
            _local b << a + 2
            write(b)
        _endmethod
        $
        """;
    // Line 1: pragma, Line 2: _method, Line 3: _local a << 1,
    // Line 4: _local b << a + 2, Line 5: write(b), Line 6: _endmethod, Line 7: $
    // Select `a + 2` on line 4, columns 16-21.
    final Range range = new Range(new Position(4, 16), new Position(4, 21));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(1);

    // a (defined before, used within) → parameter; classify_level=restricted stays restricted.
    final TextEdit replaceEdit =
        new TextEdit(
            new Range(new Position(4, 16), new Position(4, 21)), "_self.extracted_method(a)");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(8, 0), new Position(8, 0)),
            """

            _pragma(classify_level=restricted, topic={tests})
            _private _method my_object.extracted_method(a)
              ## @param {sw:integer} a
              _return a + 2
            _endmethod
            $
            """);
    // callText = "_self.extracted_method(a)", indexOf("extracted_method") = 6
    // renameColumn = 16 + 6 + 1 = 23
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 4, 23));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to method",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }
}
