package nl.ramsolutions.sw.magik.formatting;

/** Tests for {@link RelativeIndentStrategy}. */
class RelativeIndentStrategyTest extends IndentStrategyTest {

  @Override
  IndentStrategy createStrategy(final FormattingOptions options) {
    return new RelativeIndentStrategy(options);
  }

  // @Test
  // void testOk() {
  //   final String code =
  //       """
  //       _proc()
  //       	_self()
  //       _endproc
  //       """;

  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty();
  // }

  // @ParameterizedTest
  // @ValueSource(
  //     strings = {
  //       """
  //       _block
  //       print(1)
  //       _endblock
  //       """,
  //       """
  //       _block
  //       a << 2
  //       _endblock
  //       """,
  //       """
  //       _block
  //       # comment
  //       _endblock
  //       """,
  //     })
  // void testIndentBlockStatement(final String code) {
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits)
  //       .containsExactly(
  //           new TextEdit(
  //               new Range(new Position(2, 0), new Position(2, 0)), "\t", "improper indenting"));
  // }

  // @Test
  // void testIndentingAndifExpression() {
  //   final String code =
  //       """
  //       _if a() _andif
  //       b()
  //       _then
  //       c()
  //       _endif
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits)
  //       .containsExactly(
  //           new TextEdit(
  //               new Range(new Position(2, 0), new Position(2, 0)), "    ", "improper indenting"),
  //           new TextEdit(
  //               new Range(new Position(4, 0), new Position(4, 0)), "\t", "improper indenting"));
  // }

  // @Test
  // void testIndentCommentsAfterStatement() { // NOSONAR: Don't group tests.
  //   final String code =
  //       """
  //       _method a.b(a, b, c)
  //       	print(1) # test method
  //       _endmethod
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty();
  // }

  // @Test
  // void testIndentAssignmentExpression2() {
  //   final String code =
  //       """
  //       a << _if x?
  //       _then
  //         >> 1
  //       _elif x?
  //       _then
  //         >> 2
  //       _else
  //         >> 3
  //       _endif
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits)
  //       .containsExactly(
  //           new TextEdit(
  //               new Range(new Position(2, 0), new Position(2, 0)), "     ", "improper
  // indenting"),
  //           new TextEdit(
  //               new Range(new Position(3, 0), new Position(3, 2)), "\t     ", "improper
  // indenting"),
  //           new TextEdit(
  //               new Range(new Position(4, 0), new Position(4, 0)), "     ", "improper
  // indenting"),
  //           new TextEdit(
  //               new Range(new Position(5, 0), new Position(5, 0)), "     ", "improper
  // indenting"),
  //           new TextEdit(
  //               new Range(new Position(6, 0), new Position(6, 2)), "\t     ", "improper
  // indenting"),
  //           new TextEdit(
  //               new Range(new Position(7, 0), new Position(7, 0)), "     ", "improper
  // indenting"),
  //           new TextEdit(
  //               new Range(new Position(8, 0), new Position(8, 2)), "\t     ", "improper
  // indenting"),
  //           new TextEdit(
  //               new Range(new Position(9, 0), new Position(9, 0)), "     ", "improper
  // indenting"));
  // }

  // @Test
  // void testIndentArguments1() {
  //   final String code =
  //       """
  //       def_slotted_exemplar(
  //       	:test_ex,
  //       	{
  //       		{:slot1, _unset}
  //       	})
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty();
  // }

  // @Test
  // void testIndentArguments2() {
  //   final String code =
  //       """
  //       property_list.new_with(
  //       	:key1, :value1,
  //       	:key2, :value2)
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty();
  // }

  // @Test
  // void testIndentArguments2a() {
  //   final String code =
  //       """
  //       property_list.new_with(
  //       	:key1, {
  //       		:value1
  //       	},
  //       	:key2, {
  //       		:value2
  //       	})
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty();
  // }

  // @Test
  // void testIndentArguments2b() {
  //   final String code =
  //       """
  //       property_list.new_with(
  //       	:key1,
  //       	{
  //       		:value1,
  //       		:value2
  //       	},
  //       	:key2,
  //       	{
  //       		:value1,
  //       		:value2
  //       	})
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty();
  // }

  // @Test
  // void testIndentArguments4() {
  //   final String code =
  //       """
  //       call_me({
  //       	:value
  //       })
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty();
  // }

  // // @Test
  // // void testIndentArgumentsLineStart() {
  // //   final String code =
  // //       """
  // //       call_me_too(:test_1,
  // //       	    :test_2)
  // //       """;
  // //   final List<TextEdit> edits = this.getEdits(code);
  // //   assertThat(edits).isEmpty(); // TODO!
  // // }

  // @Test
  // void testIndentIfElif() {
  //   final String code =
  //       """
  //       _if a
  //       _then
  //       	show(:a)
  //       _elif b
  //       _then
  //       	show(:b)
  //       _else
  //       	show(:c)
  //       _endif
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty();
  // }

  // @Test
  // void testIndentVariableDefinitionAssignment() {
  //   final String code =
  //       """
  //       _local a <<
  //       	10
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty();
  // }

  // @Test
  // void testBinaryExpressionMultiple() {
  //   final String code =
  //       """
  //       _if a? _andif
  //       	b? _andif
  //       	c?
  //       _then
  //       	do()
  //       _endif
  //       """;
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits).isEmpty();
  // }

  // @ParameterizedTest
  // @ValueSource(
  //     strings = {
  //       """
  //       _local a << {
  //       10
  //       }
  //       """,
  //       """
  //       _local a << {{{
  //       10
  //       }}}
  //       """,
  //       """
  //       rope.new_with(
  //       10
  //       )
  //       """,
  //     })
  // void testPreventDoubleIndent(final String code) {
  //   final List<TextEdit> edits = this.getEdits(code);
  //   assertThat(edits)
  //       .containsExactly(
  //           new TextEdit(
  //               new Range(new Position(2, 0), new Position(2, 0)), "\t", "improper indenting"));
  // }
}
