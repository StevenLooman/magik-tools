package nl.ramsolutions.sw.magik.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.AstNodeHelper;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FormattingWalkerTest {

  private List<TextEdit> getEdits(final String code) {
    final FormattingOptions options = new FormattingOptions(8, false, false, true, false);
    return this.getEdits(code, options);
  }

  private List<TextEdit> getEdits(final String code, final FormattingOptions options) {
    final MagikFile magikFile = new MagikFile(MagikTypedFile.DEFAULT_URI, code);
    final FormattingProvider formattingProvider =
        new FormattingProvider(options, NullIndentWalker.class);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode topNodeClone = AstNodeHelper.clone(topNode);
    return formattingProvider.format(topNodeClone);
  }

  // region: Whitespace
  @Test
  void testWhitespaceMethodDefinition1() {
    final String code =
        """
        _method a. b(x, y, z)
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 10), new Position(1, 11)), ""));
  }

  @Test
  void testWhitespaceMethodDefinition2() {
    final String code =
        """
        _method a.b (x, y, z)
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 11), new Position(1, 12)), ""));
  }

  @Test
  void testWhitespaceMethodDefinition3() {
    final String code =
        """
        _private _method a.b()
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
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
        .containsExactly(new TextEdit(new Range(new Position(1, 14), new Position(1, 14)), " "));
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
        .containsExactly(new TextEdit(new Range(new Position(1, 17), new Position(1, 17)), " "));
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
        .containsExactly(new TextEdit(new Range(new Position(1, 16), new Position(1, 17)), ""));
  }

  @Test
  void testWhitespaceParameters4() {
    final String code = "print(a,b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 8), new Position(1, 8)), " "));
  }

  @Test
  void testWhitespaceParameters5() {
    final String code = "print(a, b,c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 11), new Position(1, 11)), " "));
  }

  @Test
  void testWhitespaceMethodInvocation1() {
    final String code = "class .method(a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 5), new Position(1, 6)), ""));
  }

  @Test
  void testWhitespaceMethodInvocation2() {
    final String code = "class. method(a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 6), new Position(1, 7)), ""));
  }

  @Test
  void testWhitespaceMethodInvocation3() {
    final String code = "class.method (a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 12), new Position(1, 13)), ""));
  }

  @Test
  void testWhitespaceArguments1() {
    final String code = "prc( a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 4), new Position(1, 5)), ""));
  }

  @Test
  void testWhitespaceArguments2() {
    final String code = "prc(a,b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 6), new Position(1, 6)), " "));
  }

  @Test
  void testWhitespaceArguments3() {
    final String code = "prc(a, b,c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 9), new Position(1, 9)), " "));
  }

  @Test
  void testWhitespaceArguments4() {
    final String code = "prc(a, b , c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 8), new Position(1, 9)), ""));
  }

  @Test
  void testWhitespaceArgumentsSelf() { // NOSONAR
    final String code = "prc(_self)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
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

  @ParameterizedTest
  @ValueSource(
      strings = {
        "_proc @b(x, y, z) _endproc",
        "_proc(x, y, z) _endproc",
      })
  void testProcDefinitionParameters(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testLoopbodyDefinitionParameters() {
    final String code = "_loopbody(x, y, z)";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testSuperDefinitionParameters() {
    final String code = "_super(mysuper)";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testSelfExpression() {
    final String code = "_self - 1";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testSelfIndexer() {
    final String code = "_self[1]";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testWhitespaceAfterTransmitBeforePragma() {
    final String code =
        """
        $

         _pragma(classify_level=debug)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 1), new Position(3, 1)), "\n\n\n"));
  }

  // endregion

  // region: Trimming
  @Test
  void testNoTrimTrailingWhitespaceStatement() {
    final String code = "a    \n";
    final FormattingOptions options = new FormattingOptions(8, false, false, false, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }

  @Test
  void testTrimTrailingWhitespaceStatement() {
    final String code = "a  \n";
    final FormattingOptions options = new FormattingOptions(8, false, false, true, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 1), new Position(2, 0)), "\n"));
  }

  @Test
  void testNoTrimTrailingWhitespaceComment() {
    final String code = "# comment  \n";
    final FormattingOptions options = new FormattingOptions(8, false, false, false, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }

  @Test
  void testTrimTrailingWhitespaceComment() {
    final String code = "# comment  \n";
    final FormattingOptions options = new FormattingOptions(8, false, false, true, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 0), new Position(2, 0)), "# comment\n"));
  }

  // endregion

  // region: Min max newlines
  @Test
  void testRequireNewlineAfterTransmit() {
    final String code =
        """
        _package user
        $
        def_slotted_exemplar(:a, {})
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(2, 1), new Position(3, 0)), "\n\n"));
  }

  @Test
  void testRequireNewlineAfterTransmitPlusNoIdent() {
    final String code =
        """
        _package user
        $
        def_slotted_exemplar(:a, {})
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(2, 1), new Position(3, 0)), "\n\n"));
  }

  @Test
  void testMultipleWhitelines1() {
    final String code =
        """
        _package user


        def_slotted_exemplar(:a, {})
        $
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 13), new Position(4, 0)), "\n\n"));
  }

  @Test
  void testMultipleWhitelines2() {
    final String code =
        """
        $



        _method a.a(parameter)
        _endmethod
        $
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 1), new Position(5, 0)), "\n\n"));
  }

  @Test
  void testMultipleWhitelines3() {
    final String code =
        """
        $



        _pragma(classify_level=basic)
        _method a.a(parameter)
        _endmethod
        $
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 1), new Position(5, 0)), "\n\n"));
  }

  @Test
  void testSingleWhitelineMethodDoc() {
    final String code =
        """
        _method object.method(param)
        	##

        	>> param + 1
        _endmethod
        $
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  // endregion

  // region: Final newlines
  @Test
  void testInsertFinalNewlinePresent() {
    final String code = "1 + 1\n";
    final FormattingOptions options = new FormattingOptions(8, false, true, false, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }

  @Test
  void testInsertFinalNewlineAbsent() {
    final String code = "1 + 1";
    final FormattingOptions options = new FormattingOptions(8, false, true, false, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 5), new Position(1, 5)), "\n"));
  }

  @Test
  void testTrimFinalNewlinesAbsent() {
    final String code = "1 + 1";
    final FormattingOptions options = new FormattingOptions(8, false, false, false, true);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }

  @Test
  void testTrimFinalNewlinesPresent() {
    final String code =
        """
        1 + 1

        """;
    final FormattingOptions options = new FormattingOptions(8, false, false, false, true);
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
    final FormattingOptions options = new FormattingOptions(8, false, false, true, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 0), new Position(2, 0)), "# comment\r\n"));
  }

  // region: Pragma
  @Test
  void testFormatPragma() {
    final String code = "_pragma(a=b,c=d,e={f,g})\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 12), new Position(1, 12)), " "),
            new TextEdit(new Range(new Position(1, 16), new Position(1, 16)), " "),
            new TextEdit(new Range(new Position(1, 21), new Position(1, 21)), " "));
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
            new TextEdit(new Range(new Position(1, 1), new Position(1, 1)), " "),
            new TextEdit(new Range(new Position(1, 2), new Position(1, 2)), " "));
  }

  @Test
  void testParenExpression() {
    final String code = "( a _andif b )";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 1), new Position(1, 2)), ""),
            new TextEdit(new Range(new Position(1, 12), new Position(1, 13)), ""));
  }

  @Test
  void testBinaryExpression() {
    final String code = "a+b\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 1), new Position(1, 1)), " "),
            new TextEdit(new Range(new Position(1, 2), new Position(1, 2)), " "));
  }

  @Test
  void testSlotExpression() {
    final String code = "1 + . slot";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 5), new Position(1, 6)), ""));
  }

  @Test
  void testAugmentedAssignment() {
    final String code = "a +<< 100";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testMultExpressionAdd() {
    final String code =
        """
        1 * 2 +
        3
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  // endregion

  @Test
  void testLabel() { // NOSONAR
    final String code = "@label";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testMultipleAssignmentTuple() { // NOSONAR
    final String code =
        """
        (first, second, third) << (_scatter data)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }
}
