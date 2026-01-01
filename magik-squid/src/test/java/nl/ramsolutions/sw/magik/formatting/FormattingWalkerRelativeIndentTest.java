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

/**
 * Test cases for {@link RelativeIndentWalker}.
 *
 * <p>Note that the tab width is set to 8 spaces (default) in these tests, to match the Magik
 * default.
 */
class FormattingWalkerRelativeIndentTest {

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
        .containsExactly(new TextEdit(new Range(new Position(2, 0), new Position(2, 0)), "\t"));
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
        .containsExactly(new TextEdit(new Range(new Position(2, 0), new Position(2, 0)), "\t"));
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
            new TextEdit(new Range(new Position(2, 0), new Position(2, 0)), "    "),
            new TextEdit(new Range(new Position(4, 0), new Position(4, 0)), "\t"));
  }

  @Test
  void testIndentCommentsAfterStatement() {
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
            new TextEdit(new Range(new Position(2, 0), new Position(2, 0)), "     "),
            new TextEdit(new Range(new Position(3, 0), new Position(3, 0)), "\t   "),
            new TextEdit(new Range(new Position(4, 0), new Position(4, 0)), "     "),
            new TextEdit(new Range(new Position(5, 0), new Position(5, 0)), "\t   "),
            new TextEdit(new Range(new Position(6, 0), new Position(6, 0)), "     "));
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
        	b.method(
        		x,
        		y,
        		z)
        """,
        """
        a[
        	x,
        	y,
        	z]
        """,
        """
        a[x,
          y,
          z]
        """,
        """
        a[x
          , y
          , z]
        """,
        """
        a <<
        	b[
        		x,
        		y,
        		z]
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

  @ParameterizedTest
  @ValueSource(
      strings = {
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
        """,
        """
        _if a
        _then
        	_if b
        	_then
        		show(:b)
        	_endif
        _endif
        """,
        """
        _if a _then show(:a)
        _elif b _then show(:b)
        _else show(:c)
        _endif
        """,
      })
  void testIndentIfElif(final String code) {
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
        """,
        """
        _constant a <<
        	10
        """,
        """
        _global a <<
        	10
        """,
        """
        _dynamic a <<
        	10
        """,
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

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        obj.
        	method1().
        	method2().
        	method3()
        """,
        """
        obj.method1(
        	a,
        	b).
        	method2()
        """,
        """
        a <<
        	obj.
        	method1().
        	method2()
        """,
      })
  void testIndentFluentInterface(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        a << obj.method(
        	x,
        	y)
        """,
        """
        _local a << obj.method(
        	x,
        	y)
        """,
        """
        a << obj.
        	method()
        """,
        """
        (a, b) << obj.method(
        	x,
        	y)
        """,
      })
  void testIndentMethodInvocationWithAssignment(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        a.call(10) << 20
        """,
        """
        a.call(10) <<
        	20
        """,
        """
        a.call(
        	10) <<
        	20
        """,
        """
        a[1] << 20
        """,
        """
        a[1] <<
        	20
        """,
        """
        a[
        	1] <<
        	20
        """,
      })
  void testIndentAssignmentToMethodInvocation(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        a << _proc()
             _endproc
        """,
        """
        _local a << _proc()
        	    _endproc
        """,
        """
        a << _for i _over 1.upto(20)
             _loop
             _endloop
        """,
        """
        _local a << _for i _over 1.upto(20)
        	    _loop
        	    _endloop
        """,
        """
        a << _over 1.upto(20)
             _loop
             _endloop
        """,
        """
        _local a << _over 1.upto(20)
        	    _loop
        	    _endloop
        """,
        """
        a << _loop
             _endloop
        """,
        """
        _local a << _loop
        	    _endloop
        """,
        """
        _local a << _for i _over 1.upto(20) _loop
        	    _endloop
        """,
      })
  void testIndentAssignmentConstructSameLine(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        a <<
        	_proc()
        	_endproc
        """,
        """
        _local a <<
        	_proc()
        	_endproc
        """,
        """
        a <<
        	_for i _over 1.upto(20)
        	_loop
        	_endloop
        """,
        """
        _local a <<
        	_for i _over 1.upto(20)
        	_loop
        	_endloop
        """,
        """
        a <<
        	_over 1.upto(20)
        	_loop
        	_endloop
        """,
        """
        _local a <<
        	_over 1.upto(20)
        	_loop
        	_endloop
        """,
        """
        a <<
        	_loop
        	_endloop
        """,
        """
        _local a <<
        	_loop
        	_endloop
        """,
      })
  void testIndentAssignmentConstructNextLine(final String code) {
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
        .containsExactly(new TextEdit(new Range(new Position(2, 0), new Position(2, 0)), "\t"));
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
        .containsExactly(new TextEdit(new Range(new Position(1, 0), new Position(1, 1)), ""));
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
        .containsExactly(new TextEdit(new Range(new Position(2, 0), new Position(2, 1)), ""));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _if a? _andif
            b? _andif
            c?
        _then
        	do()
        _endif
        """,
        """
        _if a? _orif
            b? _orif
            c?
        _then
        	do()
        _endif
        """,
        """
        _if a? _andif
            b? _orif
            c?
        _then
        	do()
        _endif
        """,
        """
        _if a? _xor
            b?
        _then
        	do()
        _endif
        """,
      })
  void testBinaryExpressionMultiple(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testLineUpConditionalExpression() {
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
  void testLineUpAndExpression() {
    final String code =
        """
        x? <<
        	a = b _andif
        	c = d
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
        .containsExactly(new TextEdit(new Range(new Position(2, 0), new Position(2, 1)), ""));
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
  void testForFinally() {
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

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _protect
        	1
        _protection
        	2
        _endprotect
        """,
        """
        _try
        	do_something()
        _when error
        	handle_error()
        _endtry
        """,
        """
        _try _with cond
        	do_something()
        _when warning
        	handle_warning()
        _when error
        	handle_error()
        _endtry
        """,
        """
        _catch :tag
        	do_something()
        _endcatch
        """,
        """
        _lock _self
        	do_something()
        _endlock
        """,
      })
  void testProtect(final String code) {
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
        .containsExactly(new TextEdit(new Range(new Position(2, 0), new Position(2, 0)), "\t"));
  }

  @Test
  void testProcDefinitionAsArgument() {
    final String code =
        """
        coll.select(predicate.using(
        	_proc(obj) _endproc))
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _proc(param)
        	## @param {sw:integer} param Test param.
        _endproc
        """,
        """
        _local prc <<
        	_proc(param)
        		## @param {sw:integer} param Test param.
        	_endproc
        """,
        """
        _self.do(
        	_proc(param)
        		## @param {sw:integer} param Test param.
        	_endproc)
        """,
      })
  void testProcDefinitionWithComment(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testMethodIf() {
    final String code =
        """
        _method a.a
        	_if a?
        	_then
        	_endif
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testNestingForIf() {
    final String code =
        """
        _for i _over 1.upto(20)
        _loop
        	_if a?
        	_then
        	_endif
        _endloop
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testAssignSimpleVectorWithExpression() {
    final String code =
        """
        _local a << {
        	b + 1
        }
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
        	_block
        		do_something()
        	_endblock
        _endmethod
        """,
        """
        _method a.b
        	_try
        		do_something()
        	_when error
        		handle_error()
        	_endtry
        _endmethod
        """,
      })
  void testNestedConstructsInMethod(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _loop
        	_leave
        _endloop
        """,
        """
        _loop @outer
        	_loop
        		_leave @outer
        	_endloop
        _endloop
        """,
        """
        _loop
        	_continue
        _endloop
        """,
        """
        _loop @outer
        	_loop
        		_continue @outer
        	_endloop
        _endloop
        """,
        """
        _loop
        	_leave _with result
        _endloop
        """,
      })
  void testLeaveAndContinue(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _handling error _with _default
        _block
        	do_something()
        _endblock
        """,
        """
        _handling warning, error _with
        	_proc(cond)
        		write(cond)
        	_endproc
        _block
        	do_something()
        _endblock
        """,
        """
        _handling error
        	_with _default
        _block
        	do_something()
        _endblock
        """,
      })
  void testHandling(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        (a, b, c) << (1, 2, 3)
        """,
        """
        _local (a, b) << obj.method()
        """,
      })
  void testMultiValueAssignment(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _throw :error
        """,
        """
        _throw :error _with details
        """,
        """
        _block
        	_throw :error
        _endblock
        """,
        """
        _catch :error
        	_throw :error _with "message"
        _endcatch
        """,
      })
  void testThrow(final String code) {
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }
}
