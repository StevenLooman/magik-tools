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
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.formatting.MagikFormattingSettings;
import org.junit.jupiter.api.Test;

/** Test ExtractMethodCodeActionProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class ExtractMethodCodeActionProviderTest {

  /** Formatting properties using 2-space indentation, matching the expected strings in tests. */
  private static final MagikToolsProperties SPACE_PROPERTIES =
      new MagikToolsProperties(
          Map.of(
              MagikFormattingSettings.KEY_MAGIK_FORMATTING_INDENT_CHAR, "space",
              MagikFormattingSettings.KEY_MAGIK_FORMATTING_INDENT_WIDTH, "2"));

  private List<CodeAction> provideCodeActions(
      final String code, final Range range, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(SPACE_PROPERTIES, MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final ExtractMethodCodeActionProvider provider = new ExtractMethodCodeActionProvider();
    return provider.provideCodeActions(magikFile, range);
  }

  private List<CodeAction> provideCodeActions(final String code, final Range range) {
    return provideCodeActions(code, range, new DefinitionKeeper());
  }

  // --- Extract to method tests ---

  @Test
  void testExtractSingleStatementToMethod() {
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
            _local b << a + 2
            write(b)
        _endmethod
        $
        """;
    // Select line 3: "_local b << a + 2"
    final Range range = new Range(new Position(3, 4), new Position(3, 22));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(2);

    // a (defined before) → parameter; b (defined inside, used after) → return value.
    final TextEdit replaceEdit =
        new TextEdit(
            new Range(new Position(3, 4), new Position(3, 21)), "b << _self.extracted_method(a)");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(7, 0), new Position(7, 0)),
            """

            _private _method my_object.extracted_method(a)
              ## @param {sw:integer} a
              _local b << a + 2
              _return b
            _endmethod
            $
            """);
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 3, 16));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to method",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  @Test
  void testExtractMultipleStatementsToMethod() {
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
            _local b << 2
            _local c << a + b
            write(c)
        _endmethod
        $
        """;
    // Select lines 3-4: "_local b << 2" and "_local c << a + b"
    final Range range = new Range(new Position(3, 4), new Position(4, 22));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(2);

    // a (defined before) → parameter; c (defined inside, used after) → return value.
    final TextEdit replaceEdit =
        new TextEdit(
            new Range(new Position(3, 4), new Position(4, 21)), "c << _self.extracted_method(a)");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(8, 0), new Position(8, 0)),
            """

            _private _method my_object.extracted_method(a)
              ## @param {sw:integer} a
              _local b << 2
              _local c << a + b
              _return c
            _endmethod
            $
            """);
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 3, 16));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to method",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  @Test
  void testExtractWithParameters() {
    final String code =
        """
        _method my_object.test_method()
            _local x << 10
            _local y << x + 1
        _endmethod
        $
        """;
    // Select line 3: "_local y << x + 1" — x is from outer scope, should become a parameter.
    final Range range = new Range(new Position(3, 4), new Position(3, 22));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(2);

    // x (defined before) → parameter; y (defined inside, not used after) → no return value.
    final TextEdit replaceEdit =
        new TextEdit(
            new Range(new Position(3, 4), new Position(3, 21)), "_self.extracted_method(x)");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(6, 0), new Position(6, 0)),
            """

            _private _method my_object.extracted_method(x)
              ## @param {sw:integer} x
              _local y << x + 1
            _endmethod
            $
            """);
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 3, 11));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to method",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  @Test
  void testExtractWithReturnValue() {
    final String code =
        """
        _method my_object.test_method()
            _local x << 5
            write(x)
        _endmethod
        $
        """;
    // Select line 2: "_local x << 5" — x is used after the selection, should become a return.
    final Range range = new Range(new Position(2, 4), new Position(2, 18));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(2);

    // x (defined inside, used after) → return value; no parameters.
    final TextEdit replaceEdit =
        new TextEdit(
            new Range(new Position(2, 4), new Position(2, 17)), "x << _self.extracted_method");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(6, 0), new Position(6, 0)),
            """

            _private _method my_object.extracted_method
              ## @return {sw:integer}
              _local x << 5
              _return x
            _endmethod
            $
            """);
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 2, 16));

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
  void testRejectReturnStatement() {
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
            _return a
        _endmethod
        $
        """;
    // Select line 3: "_return a" — should be rejected.
    final Range range = new Range(new Position(3, 4), new Position(3, 14));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).isEmpty();
  }

  @Test
  void testRejectLeaveStatement() {
    final String code =
        """
        _method my_object.test_method()
            _loop
                _leave
            _endloop
        _endmethod
        $
        """;
    // Select only the bare _leave — no enclosing loop within the selection → rejected.
    final Range range = new Range(new Position(3, 8), new Position(3, 15));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).isEmpty();
  }

  @Test
  void testAllowLeaveStatementInsideSelectedLoop() {
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
            _loop
                _local b << a + 1
                _if b > 10
                _then
                    _leave
                _endif
                a << b
            _endloop
        _endmethod
        $
        """;
    // Select the entire _loop statement (lines 3-10). The _leave is scoped to the selected loop
    // and is therefore valid inside the extracted method.
    final Range range = new Range(new Position(3, 4), new Position(10, 12));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(2);
    assertThat(actions.get(0).getTitle()).isEqualTo("Extract to method");
    assertThat(actions.get(1).getTitle()).isEqualTo("Extract to local procedure");
  }

  @Test
  void testRejectContinueStatement() {
    final String code =
        """
        _method my_object.test_method()
            _loop
                _continue
            _endloop
        _endmethod
        $
        """;
    final Range range = new Range(new Position(3, 8), new Position(3, 18));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).isEmpty();
  }

  @Test
  void testExtractWithMultipleReturnValues() {
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
            _local b << 2
            write(a)
            write(b)
        _endmethod
        $
        """;
    // Select lines 2-3: both a and b defined inside and used after → two return values.
    final Range range = new Range(new Position(2, 4), new Position(3, 18));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(2);

    // No parameters; a and b both escape → (a, b) << _self.extracted_method
    final TextEdit replaceEdit =
        new TextEdit(
            new Range(new Position(2, 4), new Position(3, 17)), "(a, b) << _self.extracted_method");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(8, 0), new Position(8, 0)),
            """

            _private _method my_object.extracted_method
              ## @return {sw:integer}
              ## @return {sw:integer}
              _local a << 1
              _local b << 2
              _return (a, b)
            _endmethod
            $
            """);
    // "(a, b) << _self.".length() == 16, so renameColumn = 4 + 16 + 1 = 21
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 2, 21));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to method",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  @Test
  void testRejectNoStatementsSelected() {
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
        _endmethod
        $
        """;
    // Range that doesn't cover any complete statement.
    final Range range = new Range(new Position(2, 15), new Position(2, 16));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).isEmpty();
  }

  @Test
  void testRejectOutsideMethod() {
    final String code =
        """
        write("hello")
        $
        """;
    final Range range = new Range(new Position(1, 0), new Position(1, 15));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).isEmpty();
  }

  // --- Extract to proc tests ---

  @Test
  void testExtractStatementFromNestedBody() {
    final String code =
        """
        _method my_object.test_method()
            _protect
                _local a << 1
                _local b << a + 2
                write(b)
            _protection
            _endprotect
        _endmethod
        $
        """;
    // Line 4: "_local b << a + 2" is inside a _protect block, NOT a direct child of the method
    // body. The selection starts at col 0 (before the indentation). Both the nested-body lookup
    // and the leading-whitespace handling must work together.
    final Range range = new Range(new Position(4, 0), new Position(5, 0));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(2);
    assertThat(actions.get(0).getTitle()).isEqualTo("Extract to method");
    assertThat(actions.get(1).getTitle()).isEqualTo("Extract to local procedure");
  }

  @Test
  void testExtractStatementFromNestedBodyUsesOuterVariable() {
    // Variables defined OUTSIDE the nested block (before _protect) must still be detected as
    // parameters when extracting code from inside the block. This requires scope analysis to use
    // the top-level method body scope, not just the nested block's scope.
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
            _protect
                _local b << a + 2
                write(b)
            _protection
            _endprotect
        _endmethod
        $
        """;
    // Select line 4: "_local b << a + 2" — a is defined ABOVE the _protect (outer scope).
    final Range range = new Range(new Position(4, 8), new Position(4, 26));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(2);

    // a (defined before, in outer scope) → parameter. b (defined inside, used after) → return.
    final TextEdit replaceEdit =
        new TextEdit(
            new Range(new Position(4, 8), new Position(4, 25)), "b << _self.extracted_method(a)");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(10, 0), new Position(10, 0)),
            """

            _private _method my_object.extracted_method(a)
              ## @param {sw:integer} a
              _local b << a + 2
              _return b
            _endmethod
            $
            """);
    // "b << _self.extracted_method(a)".indexOf("extracted_method") = 11; col = 8 + 11 + 1 = 20
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 4, 20));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to method",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  // --- Extract to proc tests ---

  @Test
  void testExtractToProcInsideProcedure() {
    final String code =
        """
        _method my_object.test_method()
            _local my_proc << _proc()
                _local a << 1
                _local b << a + 2
                write(b)
            _endproc
            my_proc()
        _endmethod
        $
        """;
    // Select line 4: "_local b << a + 2" inside the proc.
    final Range range = new Range(new Position(4, 8), new Position(4, 26));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(1);

    // a (defined before) → parameter; b (defined inside, used after) → return value.
    final TextEdit replaceEdit =
        new TextEdit(new Range(new Position(4, 8), new Position(4, 25)), "b << extracted_proc(a)");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(4, 0), new Position(4, 0)),
            """
                    _local extracted_proc << _proc(a)
                      ## @param {sw:integer} a
                        _local b << a + 2
                        _return b
                      _endproc
            """);
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted procedure",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 9, 14));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to procedure",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  @Test
  void testExtractToProcInsideProcedureWithLeadingWhitespaceSelection() {
    // Same as testExtractToProcInsideProcedure but the selection starts at column 0 (before the
    // leading indentation). Editors often produce this when a full line is selected with
    // shift+down. The action must still target the inner PROCEDURE_DEFINITION, not the outer
    // METHOD_DEFINITION.
    final String code =
        """
        _method my_object.test_method()
            _local my_proc << _proc()
                _local a << 1
                _local b << a + 2
                write(b)
            _endproc
            my_proc()
        _endmethod
        $
        """;
    // Selection starts at column 0 (whitespace), ends at column 26 (past end of statement).
    final Range range = new Range(new Position(4, 0), new Position(4, 26));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(1);

    final TextEdit replaceEdit =
        new TextEdit(new Range(new Position(4, 8), new Position(4, 25)), "b << extracted_proc(a)");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(4, 0), new Position(4, 0)),
            """
                    _local extracted_proc << _proc(a)
                      ## @param {sw:integer} a
                        _local b << a + 2
                        _return b
                      _endproc
            """);
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted procedure",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 9, 14));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to procedure",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  @Test
  void testExtractToLocalProcFromMethod() {
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
            _local b << a + 2
            write(b)
        _endmethod
        $
        """;
    // Select line 3: "_local b << a + 2" — inside a _method body.
    final Range range = new Range(new Position(3, 4), new Position(3, 22));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(2);

    // a (defined before) → parameter; b (defined inside, used after) → return value.
    final TextEdit replaceEdit =
        new TextEdit(new Range(new Position(3, 4), new Position(3, 21)), "b << extracted_proc(a)");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(3, 0), new Position(3, 0)),
            """
                _local extracted_proc << _proc(a)
                  ## @param {sw:integer} a
                    _local b << a + 2
                    _return b
                  _endproc
            """);
    // renameLineNumber = 3 + 5 inserted lines = 8; "b << ".length() = 5 → col = 4 + 5 + 1 = 10
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted procedure",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 8, 10));

    assertThat(actions.get(1))
        .isEqualTo(
            new CodeAction(
                "Extract to local procedure",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  // --- Command test ---

  @Test
  void testCodeActionHasRenameCommand() {
    final String code =
        """
        _method my_object.test_method()
            _local a << 1
        _endmethod
        $
        """;
    final Range range = new Range(new Position(2, 4), new Position(2, 18));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(2);

    // a (defined inside, not used after) → no params, no return value.
    final TextEdit replaceEdit =
        new TextEdit(new Range(new Position(2, 4), new Position(2, 17)), "_self.extracted_method");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(5, 0), new Position(5, 0)),
            """

            _private _method my_object.extracted_method
              _local a << 1
            _endmethod
            $
            """);
    // renameColumn = startCol(4) + offset-of-"extracted_method"-in-"_self."(6) + 1-based(1) = 11
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 2, 11));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to method",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }

  @Test
  void testExtractToMethodCopiesAndAdjustsPragma() {
    final String code =
        """
        _pragma(classify_level=basic, topic={tests})
        _method my_object.test_method()
            _local a << 1
        _endmethod
        $
        """;
    // Line 1: pragma, Line 2: _method, Line 3: _local a << 1, Line 4: _endmethod, Line 5: $
    // Select line 3: "_local a << 1" — no params, no return value.
    final Range range = new Range(new Position(3, 4), new Position(3, 18));
    final List<CodeAction> actions = this.provideCodeActions(code, range);

    assertThat(actions).hasSize(2);

    // a is defined inside and not used after — no params, no return value.
    // classify_level=basic is downgraded to restricted for the private extracted method.
    final TextEdit replaceEdit =
        new TextEdit(new Range(new Position(3, 4), new Position(3, 17)), "_self.extracted_method");
    final TextEdit insertEdit =
        new TextEdit(
            new Range(new Position(6, 0), new Position(6, 0)),
            """

            _pragma(classify_level=restricted, topic={tests})
            _private _method my_object.extracted_method
              _local a << 1
            _endmethod
            $
            """);
    final CodeAction.Command renameCommand =
        new CodeAction.Command(
            "magik.triggerRename",
            "Rename extracted method",
            List.of(MagikTypedFile.DEFAULT_URI.toString(), 3, 11));

    assertThat(actions.get(0))
        .isEqualTo(
            new CodeAction(
                "Extract to method",
                List.of(replaceEdit, insertEdit),
                CodeAction.KIND_REFACTOR_EXTRACT,
                renameCommand));
  }
}
