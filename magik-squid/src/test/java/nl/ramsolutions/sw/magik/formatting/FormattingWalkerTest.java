package nl.ramsolutions.sw.magik.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FormattingWalkerTest {

  private List<TextEdit> getEdits(final String code) {
    final FormattingOptions options = new FormattingOptions(8, false, false, true, false);
    return this.getEdits(code, options);
  }

  private List<TextEdit> getEdits(final String code, final FormattingOptions options) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final FormattingWalker walker = new FormattingWalker(options);
    final AstNode topNode = magikFile.getTopNode();
    walker.walkAst(topNode);
    return walker.getTextEdits();
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 10), new Position(1, 11)),
                "",
                "no whitespace before allowed"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 11), new Position(1, 12)),
                "",
                "no whitespace before allowed"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 14), new Position(1, 14)),
                " ",
                "whitespace before required"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 17), new Position(1, 17)),
                " ",
                "whitespace before required"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 16), new Position(1, 17)),
                "",
                "no whitespace before allowed"));
  }

  @Test
  void testWhitespaceParameters4() {
    final String code = "print(a,b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 8), new Position(1, 8)),
                " ",
                "whitespace before required"));
  }

  @Test
  void testWhitespaceParameters5() {
    final String code = "print(a, b,c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 11), new Position(1, 11)),
                " ",
                "whitespace before required"));
  }

  @Test
  void testWhitespaceMethodInvocation1() {
    final String code = "class .method(a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 5), new Position(1, 6)),
                "",
                "no whitespace before allowed"));
  }

  @Test
  void testWhitespaceMethodInvocation2() {
    final String code = "class. method(a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 6), new Position(1, 7)),
                "",
                "no whitespace before allowed"));
  }

  @Test
  void testWhitespaceMethodInvocation3() {
    final String code = "class.method (a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 12), new Position(1, 13)),
                "",
                "no whitespace before allowed"));
  }

  @Test
  void testWhitespaceArguments1() {
    final String code = "prc( a, b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 4), new Position(1, 5)),
                "",
                "no whitespace before allowed"));
  }

  @Test
  void testWhitespaceArguments2() {
    final String code = "prc(a,b, c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 6), new Position(1, 6)),
                " ",
                "whitespace before required"));
  }

  @Test
  void testWhitespaceArguments3() {
    final String code = "prc(a, b,c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 9), new Position(1, 9)),
                " ",
                "whitespace before required"));
  }

  @Test
  void testWhitespaceArguments4() {
    final String code = "prc(a, b , c)\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 8), new Position(1, 9)),
                "",
                "no whitespace before allowed"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(2, 0), new Position(2, 0)), "\t", "improper indenting"));
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
        "_proc@b(x, y, z) _endproc",
        "_proc(x, y, z) _endproc",
      })
  void testProcDefinitionParameters(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testLoopbdoyDefinitionParameters() {
    final String code = "_loopbody(x, y, z)";
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(2, 0), new Position(2, 0)), "\t", "improper indenting"));
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
            new TextEdit(
                new Range(new Position(2, 0), new Position(2, 0)), "\t", "improper indenting"),
            new TextEdit(
                new Range(new Position(3, 0), new Position(3, 2)), "\t\t", "improper indenting"),
            new TextEdit(
                new Range(new Position(4, 0), new Position(4, 0)), "\t", "improper indenting"),
            new TextEdit(
                new Range(new Position(5, 0), new Position(5, 2)), "\t\t", "improper indenting"),
            new TextEdit(
                new Range(new Position(6, 0), new Position(6, 0)), "\t", "improper indenting"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 0), new Position(1, 1)),
                "",
                "no whitespace before allowed"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(2, 0), new Position(2, 1)), "", "improper indenting"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 1), new Position(1, 3)),
                "",
                "no whitespace after allowed"));
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
            new TextEdit(
                new Range(new Position(1, 9), new Position(1, 11)),
                "",
                "no whitespace after allowed"));
  }

  // endregion

  // region: Newlines
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(3, 0), new Position(3, 0)),
                "\n",
                "empty line before is required"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(3, 0), new Position(3, 0)),
                "\n",
                "empty line before is required"),
            new TextEdit(
                new Range(new Position(3, 0), new Position(3, 2)), "", "improper indenting"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(2, 0), new Position(3, 0)), "", "no empty line allowed"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(2, 0), new Position(3, 0)), "", "no empty line allowed"),
            new TextEdit(
                new Range(new Position(3, 0), new Position(4, 0)), "", "no empty line allowed"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(2, 0), new Position(3, 0)), "", "no empty line allowed"),
            new TextEdit(
                new Range(new Position(3, 0), new Position(4, 0)), "", "no empty line allowed"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 5), new Position(1, 5)), "\n", "final newline required"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(2, 5), new Position(4, 0)), "", "no final newline allowed"));
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
            new TextEdit(
                new Range(new Position(1, 9), new Position(1, 11)),
                "",
                "no whitespace after allowed"),
            new TextEdit(
                new Range(new Position(3, 0), new Position(3, 0)), "\t", "improper indenting"));
  }

  // region: Pragma
  @Test
  void testFormatPragma() {
    final String code = "_pragma(a=b,c=d,e={f,g})\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 12), new Position(1, 12)),
                " ",
                "whitespace before required"),
            new TextEdit(
                new Range(new Position(1, 16), new Position(1, 16)),
                " ",
                "whitespace before required"),
            new TextEdit(
                new Range(new Position(1, 21), new Position(1, 21)),
                " ",
                "whitespace before required"));
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
            new TextEdit(
                new Range(new Position(1, 1), new Position(1, 1)),
                " ",
                "whitespace before required"),
            new TextEdit(
                new Range(new Position(1, 2), new Position(1, 2)),
                " ",
                "whitespace before required"));
  }

  @Test
  void testParenExpression() {
    final String code = "( a _andif b )";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 1), new Position(1, 2)),
                "",
                "no whitespace before allowed"),
            new TextEdit(
                new Range(new Position(1, 12), new Position(1, 13)),
                "",
                "no whitespace before allowed"));
  }

  @Test
  void testBinaryExpression() {
    final String code = "a+b\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 1), new Position(1, 1)),
                " ",
                "whitespace before required"),
            new TextEdit(
                new Range(new Position(1, 2), new Position(1, 2)),
                " ",
                "whitespace before required"));
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(1, 5), new Position(1, 6)),
                "",
                "no whitespace before allowed"));
  }

  @Test
  void testAugmentedAssignment() {
    final String code = "a +<< 100";
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
}
