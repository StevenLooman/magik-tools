package nl.ramsolutions.sw.magik.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link TabbedIndentStrategy}.
 */
class TabbedIndentStrategyTest extends IndentStrategyTest {

  @Test
  void testOk() {
    final String code =
        """
        _proc()
        	_self()
        _endproc
        """;

    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
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
        # comment
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
        .containsExactly(
            new TextEdit(
                new Range(new Position(2, 0), new Position(2, 0)), "\t", "improper indenting"));
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
            new TextEdit(
                new Range(new Position(2, 0), new Position(2, 0)), "\t", "improper indenting"),
            new TextEdit(
                new Range(new Position(4, 0), new Position(4, 0)), "\t", "improper indenting"));
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
}
