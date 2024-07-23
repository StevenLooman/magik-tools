package nl.ramsolutions.sw.magik.languageserver.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test FormattingProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class FormattingProviderTest {

  private static final URI DEFAULT_URI = URI.create("memory://source.magik");

  private List<TextEdit> getEdits(final String code) {
    final FormattingOptions options = new FormattingOptions();
    return this.getEdits(code, options);
  }

  private List<TextEdit> getEdits(final String code, final FormattingOptions options) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile = new MagikTypedFile(DEFAULT_URI, code, definitionKeeper);

    final FormattingProvider provider = new FormattingProvider();
    return provider.provideFormatting(magikFile, options);
  }

  // region: Whitespace
  @Test
  void testWhitespaceMethodDefintion1() {
    final String code =
        """
        _method a. b(x, y, z)
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 10), new Position(0, 11)), ""));
  }

  @Test
  void testWhitespaceMethodDefintion2() {
    final String code =
        """
        _method a.b (x, y, z)
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 11), new Position(0, 12)), ""));
  }

  @Test
  void testWhitespaceParameters1() {
    final String code =
        """
        _method a.b(x,y, z)
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 14), new Position(0, 14)), " "));
  }

  @Test
  void testWhitespaceParameters2() {
    final String code =
        """
        _method a.b(x, y,z)
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 17), new Position(0, 17)), " "));
  }

  @Test
  void testWhitespaceParameters3() {
    final String code =
        """
        _method a.b(x, y , z)
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 16), new Position(0, 17)), ""));
  }

  @Test
  void testWhitespaceParameters4() {
    final String code = "print(a,b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 8), new Position(0, 8)), " "));
  }

  @Test
  void testWhitespaceParameters5() {
    final String code = "print(a, b,c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 11), new Position(0, 11)), " "));
  }

  @Test
  void testWhitespaceMethodInvocation1() {
    final String code = "class .method(a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 5), new Position(0, 6)), ""));
  }

  @Test
  void testWhitespaceMethodInvocation2() {
    final String code = "class. method(a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 6), new Position(0, 7)), ""));
  }

  @Test
  void testWhitespaceMethodInvocation3() {
    final String code = "class.method (a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 12), new Position(0, 13)), ""));
  }

  @Test
  void testWhitespaceArguments1() {
    final String code = "prc( a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 4), new Position(0, 5)), ""));
  }

  @Test
  void testWhitespaceArguments2() {
    final String code = "prc(a,b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 6), new Position(0, 6)), " "));
  }

  @Test
  void testWhitespaceArguments3() {
    final String code = "prc(a, b,c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 9), new Position(0, 9)), " "));
  }

  @Test
  void testWhitespaceArguments4() {
    final String code = "prc(a, b , c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 8), new Position(0, 9)), ""));
  }

  @Test
  void testWhitespaceArgumentsSelf() { // NOSONAR
    final String code = "prc(_self)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testWhitespaceMethodInvocationMultiLine() {
    final String code =
        """
        obj.
        m()
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 0), new Position(1, 0)), "\t"));
  }

  @Test
  void testWhitespaceSimpleVector() {
    final String code = "{:slot1, _unset, :readable, :public}";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testWhitespaceAssignmentMethod() {
    final String code = "_self.x() << 10";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  // endregion

  // region: Indenting.
  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _block
        print(1)
        _endblock
        """,
        """
        _block
        # comment
        _endblock
        """,
        """
        _if a() _andif
        b()
        _then _endif
        """,
        """
        _block
        a << 2
        _endblock
        """,
      })
  void testIndentBlockStatement(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 0), new Position(1, 0)), "\t"));
  }

  @Test
  void testIndentCommentsAfterStatement() { // NOSONAR: Don't group tests.
    final String code =
        """
        _method a.b(a, b, c)
        	print(1) # test method
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testIndentAssignmentExpression2() {
    final String code =
        """
        a << _if x?
        _then
          >> 1
        _else
          >> 2
        _endif""";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 0), new Position(1, 0)), "\t"),
            new TextEdit(new Range(new Position(2, 0), new Position(2, 2)), "\t\t"),
            new TextEdit(new Range(new Position(3, 0), new Position(3, 0)), "\t"),
            new TextEdit(new Range(new Position(4, 0), new Position(4, 2)), "\t\t"),
            new TextEdit(new Range(new Position(5, 0), new Position(5, 0)), "\t"));
  }

  @Test
  void testIndentArguments() {
    final String code =
        """
        def_slotted_exemplar(
        	:test_ex,
        	{
        		{:slot1, _unset}
        	})
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  // @Test
  // void testIndentArgumentsLineStart() {
  //   final String code =
  //       """
  //       call_me_too(:test_1,
  //       	    :test_2)
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty(); // TODO!
  // }

  @Test
  void testIndentIfElif() {
    final String code =
        """
        _if a
        _then
        	show(:a)
        _elif b
        _then
        	show(:b)
        _else
        	show(:c)
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testIndentVariableDefinitionAssignment() {
    final String code =
        """
        _local a <<
        	10""";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  // @Test
  // void testIndentVariableDefinitionAssignmentSimpleVector() {
  //   final String code =
  //       """
  //       _local a << {
  //       	10
  //       }""";
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty();
  // }

  // endregion

  // region: Comments
  @Test
  void testCommentsLineBefore() {
    final String code =
        """
         # comment 1
        a
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 0), new Position(0, 1)), ""));
  }

  @Test
  void testCommentsLineAfter() {
    final String code =
        """
        a
         # comment 1
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 0), new Position(1, 1)), ""));
  }

  // endregion

  // region: Trimming
  @Test
  void testNoTrimTrailingWhitespaceStatement() {
    final String code = "a    \n";
    final FormattingOptions options = new FormattingOptions();
    options.setTrimTrailingWhitespace(false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }

  @Test
  void testTrimTrailingWhitespaceStatement() {
    final String code = "a  \n";
    final FormattingOptions options = new FormattingOptions();
    options.setTrimTrailingWhitespace(true);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 1), new Position(0, 3)), ""));
  }

  @Test
  void testNoTrimTrailingWhitespaceComment() {
    final String code = "# comment  \n";
    final FormattingOptions options = new FormattingOptions();
    options.setTrimTrailingWhitespace(false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }

  @Test
  void testTrimTrailingWhitespaceComment() {
    final String code = "# comment  \n";
    final FormattingOptions options = new FormattingOptions();
    options.setTrimTrailingWhitespace(true);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 9), new Position(0, 11)), ""));
  }

  // endregion

  // region: Final newlines
  @Test
  void testInsertFinalNewlinePresent() {
    final String code = "1 + 1\n";
    final FormattingOptions options = new FormattingOptions();
    options.setInsertFinalNewline(true);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }

  @Test
  void testInsertFinalNewlineAbsent() {
    final String code = "1 + 1";
    final FormattingOptions options = new FormattingOptions();
    options.setInsertFinalNewline(true);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 5), new Position(0, 5)), "\n"));
  }

  @Test
  void testTrimFinalNewlinesAbsent() {
    final String code = "1 + 1";
    final FormattingOptions options = new FormattingOptions();
    options.setTrimFinalNewlines(true);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }

  @Test
  void testTrimFinalNewlinesPresent() {
    final String code =
        """
      1 + 1

      """;
    final FormattingOptions options = new FormattingOptions();
    options.setTrimFinalNewlines(true);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 5), new Position(3, 0)), ""));
  }

  // endregion

  @Test
  void testFormattingCrLf() {
    final String code =
        """
        # comment  \r
        _block\r
        a.do()\r
        _endblock\r
        """;
    final FormattingOptions options = new FormattingOptions();
    options.setTrimTrailingWhitespace(true);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(0, 9), new Position(0, 11)), ""),
            new TextEdit(new Range(new Position(2, 0), new Position(2, 0)), "\t"));
  }

  // region: Pragma
  @Test
  void testFormatPragma() {
    final String code = "_pragma(a=b,c=d,e={f,g})\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(0, 12), new Position(0, 12)), " "),
            new TextEdit(new Range(new Position(0, 16), new Position(0, 16)), " "),
            new TextEdit(new Range(new Position(0, 21), new Position(0, 21)), " "));
  }

  @Test
  void testFormatPragma2() { // NOSONAR
    final String code = "_pragma(a=b, c=d, e={f, g})\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  // endregion

  // region: Expressions
  @Test
  void testUnaryExpression() {
    final String code = "a+-2\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(0, 1), new Position(0, 1)), " "),
            new TextEdit(new Range(new Position(0, 2), new Position(0, 2)), " "));
  }

  @Test
  void testParenExpression() {
    final String code = "( a _andif b )";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(0, 1), new Position(0, 2)), ""),
            new TextEdit(new Range(new Position(0, 12), new Position(0, 13)), ""));
  }

  @Test
  void testBinaryExpression() {
    final String code = "a+b\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(0, 1), new Position(0, 1)), " "),
            new TextEdit(new Range(new Position(0, 2), new Position(0, 2)), " "));
  }

  @Test
  void testBinaryExpressionMultiple() {
    final String code =
        """
        _if a? _andif
        	b? _andif
        	c?
        _then
        	do()
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testSlotExpression() {
    final String code = "1 + . slot";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(0, 5), new Position(0, 6)), ""));
  }

  // endregion

  @Test
  void testLabel() { // NOSONAR
    final String code = "@label";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }
}
