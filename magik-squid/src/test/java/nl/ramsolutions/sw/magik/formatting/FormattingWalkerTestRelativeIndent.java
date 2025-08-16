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

class FormattingWalkerTestRelativeIndent {

  private List<TextEdit> getEdits(final String code) {
    final FormattingOptions options = new FormattingOptions(8, false, false, true, false);
    return this.getEdits(code, options);
  }

  private List<TextEdit> getEdits(final String code, final FormattingOptions options) {
    final MagikFile magikFile = new MagikFile(MagikTypedFile.DEFAULT_URI, code);
    final FormattingProvider formattingProvider =
        new FormattingProvider(options, RelativeIndentWalker.class);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode topNodeClone = AstNodeHelper.clone(topNode);
    return formattingProvider.format(topNodeClone);
  }

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
        a << 2
        _endblock
        """,
      })
  void testIndentBlockStatement(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 6), new Position(2, 0)), "\n\t"));
  }

  @Test
  void testIndentBlockStatementComment() {
    final String code =
        """
        _block
        # comment
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 6), new Position(3, 0)), "\n\t# comment\n"));
  }

  @Test
  void testIndentingAndifExpression() {
    final String code =
        """
        _if a() _andif
        b()
        _then
        c()
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 14), new Position(2, 0)), "\n    "),
            new TextEdit(new Range(new Position(3, 5), new Position(4, 0)), "\n\t"));
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
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 11), new Position(2, 0)), "\n     "),
            new TextEdit(new Range(new Position(2, 5), new Position(3, 2)), "\n\t     "),
            new TextEdit(new Range(new Position(3, 6), new Position(4, 0)), "\n     "),
            new TextEdit(new Range(new Position(4, 5), new Position(5, 2)), "\n\t     "),
            new TextEdit(new Range(new Position(5, 6), new Position(6, 0)), "\n     "));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        a.method(
        	x,
        	y,
        	z)
        """,
        """
        a.method1(x,
        	  y,
        	  z)
        """,
        """
        a.method(x
        	 , y
        	 , z)
        """,
        """
        a <<
        	a.method(
        		x,
        		y,
        		z)
        """,
      })
  void testIndentArguments(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testIndentArgumentsWithSimpleVector() {
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

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _local a <<
        	10
        """,
        """
        _local a
        	<<
        	10
        """,
        """
        _local
        	a <<
        	10
        """
      })
  void testIndentVariableDefinitionAssignment(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testIndentVariableDefinitionAssignmentSimpleVector() {
    final String code =
        """
        _local a << {
        		    10
        	    }
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testIndentFluentInterface() {
    final String code =
        """
        obj.
        	method1().
        	method2().
        	method3()
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testIndentAssignmentConstruct() {
    final String code =
        """
        a <<
        	_proc()
        	_endproc
        """;
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
        .containsExactly(new TextEdit(new Range(new Position(1, 4), new Position(2, 0)), "\n\t"));
  }

  @Test
  void testProcSelfInvocation() {
    final String code =
        """
        _proc()
        	_self()
        _endproc
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }
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
            new TextEdit(new Range(new Position(1, 0), new Position(2, 0)), "# comment 1\n"));
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
            new TextEdit(new Range(new Position(1, 1), new Position(3, 0)), "\n# comment 1\n"));
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
  void testLineUpConditionalExpression() { // NOSONAR
    final String code =
        """
        _if a? _is _false _andif
            b? _is _false
        _then
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testAddExpression() {
    final String code =
        """
        1 +
        	2
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(1, 3), new Position(2, 1)), "\n"));
  }

  @Test
  void testAddMultExpression() {
    final String code =
        """
        1 + 2 *
            3
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testForFinally() { // NOSONAR
    final String code =
        """
        _for i _over 1.upto(10)
        _loop
        _finally
        	_return _false
        _endloop
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testProtect() { // NOSONAR
    final String code =
        """
        _protect
        	1
        _protection
        	2
        _endprotect
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testFormattingCrLf() {
    final String code =
        """
        _block\r
        a.do()\r
        _endblock\r
        """;
    final FormattingOptions options = new FormattingOptions(8, false, false, true, false);
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits)
        .containsExactly(
            new TextEdit(new Range(new Position(1, 6), new Position(3, 0)), "\r\n\t"));
  }

  @Test
  void testProcDefinitionAsArgument() { // NOSONAR
    final String code =
        """
        coll.select(predicate.using(
        	_proc(obj) _endproc))
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }
}
