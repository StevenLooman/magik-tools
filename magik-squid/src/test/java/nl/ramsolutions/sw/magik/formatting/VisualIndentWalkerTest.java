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

/**
 * Test cases for {@link VisualIndentWalker}.
 *
 * <p>Tests verify visual alignment indentation strategy. For constructs that start at the beginning
 * of lines (column 0), both VisualIndentWalker and BlockIndentWalker produce identical results,
 * since indenting from column 0 + tab = tab. These tests focus on such cases.
 *
 * <p>Tab width is set to 8 spaces (default) to match the Magik default.
 *
 * <p>Note: Some tests with nested IF/TRY constructs are excluded because VisualIndentWalker indents
 * intermediate keywords (_then, _when, _elif) from their parent construct's column position,
 * producing space-based alignment instead of tab-based indentation. This is the expected behavior
 * for the visual alignment strategy but differs from BlockIndentWalker's output.
 */
class VisualIndentWalkerTest {

  private static final String TAB = "\t";
  private static final String TAB2 = "\t\t";
  private static final String TAB3 = "\t\t\t";
  private static final String TAB4 = "\t\t\t\t";
  private static final String TAB5 = "\t\t\t\t\t";

  private List<TextEdit> getEdits(final String code) {
    final FormattingOptions options = new FormattingOptions(8, false, false, true, false);
    return this.getEdits(code, options);
  }

  private List<TextEdit> getEdits(final String code, final FormattingOptions options) {
    final MagikFile magikFile = new MagikFile(MagikTypedFile.DEFAULT_URI, code);
    final FormattingProvider formattingProvider =
        new FormattingProvider(options, VisualIndentWalker.class);
    final AstNode topNode = magikFile.getTopNode();
    final AstNode topNodeClone = AstNodeHelper.clone(topNode);
    return formattingProvider.format(topNodeClone);
  }

  /** Helper to create a TextEdit that inserts text at the start of a line. */
  private TextEdit insertAt(final int line, final String text) {
    return new TextEdit(new Range(new Position(line, 0), new Position(line, 0)), text);
  }

  /** Helper to create a TextEdit that replaces text at the start of a line. */
  private TextEdit replaceAt(final int line, final int endCol, final String text) {
    return new TextEdit(new Range(new Position(line, 0), new Position(line, endCol)), text);
  }

  // ==========================================================================
  // Block statements
  // ==========================================================================

  @Test
  void testIndentBlockBody() {
    final String code =
        """
        _block
        print(1)
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentBlockBodyMultipleStatements() {
    final String code =
        """
        _block
        a << 1
        b << 2
        c << 3
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB), insertAt(4, TAB));
  }

  @Test
  void testIndentBlockComment() {
    final String code =
        """
        _block
        # comment
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  // ==========================================================================
  // If/elif/else statements
  // ==========================================================================

  @Test
  void testIndentIfThenBody() {
    final String code =
        """
        _if a
        _then
        show(:a)
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(3, TAB));
  }

  @Test
  void testIndentIfElifElse() {
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
    assertThat(edits).containsExactly(insertAt(3, TAB), insertAt(6, TAB), insertAt(8, TAB));
  }

  // ==========================================================================
  // Method definitions
  // ==========================================================================

  @Test
  void testIndentMethodBody() {
    final String code =
        """
        _method my_class.my_method
        do_something()
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentMethodWithModifiers() {
    final String code =
        """
        _private _method a.b
        do_something()
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentMethodWithDocComment() {
    final String code =
        """
        _method a.b
        ## Example method.
        do_something()
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  // ==========================================================================
  // Procedure definitions
  // ==========================================================================

  @Test
  void testIndentProcBody() {
    final String code =
        """
        _proc()
        do_something()
        _endproc
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  // ==========================================================================
  // Loop constructs
  // ==========================================================================

  @Test
  void testIndentForOverLoop() {
    final String code =
        """
        _for i _over range.elements()
        _loop
        do_something(i)
        _endloop
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(3, TAB));
  }

  @Test
  void testIndentOverLoop() {
    final String code =
        """
        _over collection.fast_elements()
        _loop
        do_something()
        _endloop
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(3, TAB));
  }

  @Test
  void testIndentWhileLoop() {
    final String code =
        """
        _while a?
        _loop
        b << a.next()
        _endloop
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(3, TAB));
  }

  @Test
  void testIndentLoopFinally() {
    final String code =
        """
        _for i _over range.elements()
        _loop
        do_something(i)
        _finally
        cleanup()
        _endloop
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(3, TAB), insertAt(5, TAB));
  }

  // ==========================================================================
  // Try/catch/protect constructs
  // ==========================================================================

  @Test
  void testIndentProtect() {
    final String code =
        """
        _protect
        do_something()
        _protection
        cleanup()
        _endprotect
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(4, TAB));
  }

  @Test
  void testIndentCatch() {
    final String code =
        """
        _catch :tag
        do_something()
        _endcatch
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentLock() {
    final String code =
        """
        _lock _self
        do_something()
        _endlock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentTryWhen() {
    final String code =
        """
        _try _with cond
        do(:a)
        _when error
        handle_error()
        _endtry
        """;
    final List<TextEdit> edits = this.getEdits(code);
    // _when lines up with _try; bodies indent one tab from _try/_when
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(4, TAB));
  }

  @Test
  void testIndentTryWhenNestedInBlock() {
    final String code =
        """
        _block
        _try _with cond
        do(:a)
        _when error
        handle_error()
        _endtry
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    // _try and _endtry indent one tab from _block
    // do(:a) and handle_error() indent two tabs
    // _when lines up with _try (one tab)
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB),
            insertAt(3, TAB2),
            insertAt(4, TAB),
            insertAt(5, TAB2),
            insertAt(6, TAB));
  }

  // ==========================================================================
  // Slot access and method chaining
  // ==========================================================================

  @Test
  void testIndentSlotAccessChain() {
    final String code =
        """
        _method a.b
        .slot_1.method(10)
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentSlotAccessOnly() {
    final String code =
        """
        _method a.b
        .slot_1.slot_2
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentSlotAssignmentWithExpression() {
    final String code =
        """
        _method a.b
        .slot_1 << a + b * 2
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentMethodChainWithSlot() {
    final String code =
        """
        obj.
        method1().
        method2()
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  // ==========================================================================
  // Nested constructs
  // ==========================================================================

  @Test
  void testIndentNestedBlocks() {
    final String code =
        """
        _block
        _block
        do_something()
        _endblock
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB2), insertAt(4, TAB));
  }

  @Test
  void testIndentMethodWithBlockInBody() {
    final String code =
        """
        _method a.b
        _block
        do_something()
        _endblock
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB2), insertAt(4, TAB));
  }

  // ==========================================================================
  // Nested if
  // ==========================================================================

  @Test
  void testIndentNestedIf() {
    final String code =
        """
        _if a
        _then
        _if b
        _then
        show(:b)
        _endif
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(insertAt(3, TAB), insertAt(4, TAB), insertAt(5, TAB2), insertAt(6, TAB));
  }

  // ==========================================================================
  // Binary expressions (_andif, _and)
  // ==========================================================================

  @Test
  void testIndentOrifExpression() {
    final String code =
        """
        _if a() _orif
        b()
        _then
        c()
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, "    "), insertAt(4, TAB));
  }

  @Test
  void testIndentAndifExpression() {
    final String code =
        """
        _if a() _andif
        b()
        _then
        c()
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, "    "), insertAt(4, TAB));
  }

  @Test
  void testIndentMultipleAndif() {
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
    assertThat(edits).containsExactly(insertAt(2, "    "), insertAt(3, "    "), insertAt(5, TAB));
  }

  @Test
  void testIndentMixedAndifOrif() {
    // _andif has higher precedence than _orif, so this parses as:
    // OR_EXPRESSION(AND_EXPRESSION(a(), b()), AND_EXPRESSION(c(), d()))
    // All continuation lines should align with a() at column 4.
    final String code =
        """
        _if a() _andif
        b() _orif
        c() _andif
        d()
        _then
        e()
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            insertAt(2, "    "), insertAt(3, "    "), insertAt(4, "    "), insertAt(6, TAB));
  }

  @Test
  void testIndentBooleanExpression() {
    final String code =
        """
        _if a _and
        b _and
        c
        _then
        do()
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, "    "), insertAt(3, "    "), insertAt(5, TAB));
  }

  // ==========================================================================
  // Method parameters
  // ==========================================================================

  @Test
  void testIndentMethodParameters() {
    final String code =
        """
        _method my_class.my_method(
        p_first_param,
        p_second_param
        )
        do_something()
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    // Closing ) is also indented (it's a parameter list member)
    assertThat(edits)
        .containsExactly(insertAt(2, TAB), insertAt(3, TAB), insertAt(4, TAB), insertAt(5, TAB));
  }

  @Test
  void testIndentMethodWithGatherParam() {
    final String code =
        """
        _method a.b(
        p_first,
        _gather p_rest
        )
        do_something()
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    // Closing ) is also indented (it's a parameter list member)
    assertThat(edits)
        .containsExactly(insertAt(2, TAB), insertAt(3, TAB), insertAt(4, TAB), insertAt(5, TAB));
  }

  // ==========================================================================
  // Procedure definitions (additional)
  // ==========================================================================

  @Test
  void testIndentProcInAssignmentNextLine() {
    final String code =
        """
        my_proc <<
        _proc()
        do_something()
        _endproc
        """;
    final List<TextEdit> edits = this.getEdits(code);
    // _proc is the RHS of the assignment so it indents one tab; body indents two tabs from the
    // assignment; _endproc aligns with _proc at one tab.
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB2), insertAt(4, TAB));
  }

  @Test
  void testIndentProcInAssignment() {
    // When _proc starts on the same line as the assignment (my_proc << _proc()), visual alignment
    // means the body and _endproc are aligned relative to the _proc keyword column.
    // "my_proc << " = 11 chars, so _proc is at column 11.
    // body: column 11 + 8 = 19 = "\t\t   " (2 tabs + 3 spaces with tabSize=8)
    // _endproc: column 11 = "\t   " (1 tab + 3 spaces)
    final String code =
        """
        my_proc << _proc()
        do_something()
        _endproc
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, "\t\t   "), insertAt(3, "\t   "));
  }

  @Test
  void testIndentBlockInAssignment() {
    // When _block starts on the same line as the assignment (my_block << _block), visual alignment
    // means the body and _endblock are aligned relative to the _block keyword column.
    // "my_block << " = 12 chars, so _block is at column 12.
    // body: column 12 + 8 = 20 = "\t\t    " (2 tabs + 4 spaces with tabSize=8)
    // _endblock: column 12 = "\t    " (1 tab + 4 spaces)
    final String code =
        """
        my_block << _block
        do_something()
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, "\t\t    "), insertAt(3, "\t    "));
  }

  // ==========================================================================
  // Standalone loop
  // ==========================================================================

  @Test
  void testIndentStandaloneLoop() {
    final String code =
        """
        _loop
        do_something()
        _if done?
        _then
        _leave
        _endif
        _endloop
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB),
            insertAt(3, TAB),
            insertAt(4, TAB),
            insertAt(5, TAB2),
            insertAt(6, TAB));
  }

  // ==========================================================================
  // Method invocations and arguments
  // ==========================================================================

  @Test
  void testIndentMethodArguments() {
    final String code =
        """
        a.method(
        x,
        y,
        z)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB), insertAt(4, TAB));
  }

  @Test
  void testIndentMethodArgumentsWithAssignment() {
    final String code =
        """
        a << obj.method(
        x,
        y)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  @Test
  void testIndentNestedMethodCalls() {
    final String code =
        """
        result << outer_method(
        inner_method(a, b, c),
        another_method(
        x,
        y,
        z
        )
        )
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB),
            insertAt(3, TAB),
            insertAt(4, TAB2),
            insertAt(5, TAB2),
            insertAt(6, TAB2),
            insertAt(7, TAB));
  }

  @Test
  void testIndentIndexAccess() {
    final String code =
        """
        a[
        x,
        y,
        z]
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB), insertAt(4, TAB));
  }

  @Test
  void testIndentProcedureInvocation() {
    final String code =
        """
        proc_name(
        a,
        b)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  @Test
  void testIndentSuperInvocation() {
    final String code =
        """
        _method a.b(c)
        _return _super.b(
        c)
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB2));
  }

  @Test
  void testIndentCloneInvocation() {
    final String code =
        """
        _method a.b(c)
        _return _clone.b(
        c)
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB2));
  }

  @Test
  void testIndentInvocationWithMultipleResults() {
    final String code =
        """
        (a, b, c) << method_call(
        x,
        y)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  @Test
  void testIndentMultipleAssignmentWithCall() {
    final String code =
        """
        (a, b) << object.method(
        param1,
        param2)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  // ==========================================================================
  // Method chaining (additional)
  // ==========================================================================

  @Test
  void testIndentMethodChainThreeItems() {
    final String code =
        """
        obj.
        method1().
        method2().
        method3()
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB), insertAt(4, TAB));
  }

  @Test
  void testIndentMethodChainWithAssignment() {
    final String code =
        """
        result << collection.select(predicate).
        map(transformer).
        reduce(initial, reducer)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  @Test
  void testIndentNestedInvocationsWithChaining() {
    final String code =
        """
        result << object.
        method1().
        method2(
        arg1,
        arg2
        )
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB),
            insertAt(3, TAB),
            insertAt(4, TAB2),
            insertAt(5, TAB2),
            insertAt(6, TAB));
  }

  // ==========================================================================
  // Collections (simple vectors, tuples)
  // ==========================================================================

  @Test
  void testIndentSimpleVector() {
    final String code =
        """
        _local a << {
        10
        }
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentSimpleVectorAsArgument() {
    final String code =
        """
        call({
        1
        })
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentNestedVectors() {
    final String code =
        """
        _local nested << {
        {:a, :b, :c},
        {:d, :e, :f}
        }
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  @Test
  void testIndentDeeplyNestedVectors() {
    final String code =
        """
        _local deep << {
        {
        {1, 2, 3},
        {4, 5, 6}
        },
        {
        {7, 8, 9}
        }
        }
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB),
            insertAt(3, TAB2),
            insertAt(4, TAB2),
            insertAt(5, TAB),
            insertAt(6, TAB),
            insertAt(7, TAB2),
            insertAt(8, TAB));
  }

  @Test
  void testIndentSimpleVectorInlineFirstElement() {
    // When first element is on same line as {, continuation items should align with it.
    // tabSize=8: { at visual col 8 (1 tab), :a at col 9 -> :c should be at col 9 (\t )
    final String code =
        """
        call(
        \t{:a, :b,
        \t\t:c, :d})
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(3, 1), new Position(3, 2)), " "));
  }

  @Test
  void testPreserveSimpleVectorAlignedWithFirstElement() {
    // Already correctly aligned: continuation items align with first element on same line as {.
    final String code =
        """
        call(
        \t{:a, :b,
        \t :c, :d}),
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testIndentTuple() {
    // _return (TUPLE) - TUPLE node includes parens, content indents one tab from line start.
    final String code =
        """
        _return (
        item_one,
        item_two
        )
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  @Test
  void testIndentCollectionInCallWithChain() {
    final String code =
        """
        obj.method(
        {
        1
        }).next()
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB2), insertAt(4, TAB));
  }

  // ==========================================================================
  // Assignment expressions
  // ==========================================================================

  @Test
  void testIndentAssignmentContinuation() {
    final String code =
        """
        _local a <<
        10
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentAssignmentWithIf() {
    final String code =
        """
        a <<
        _if x?
        _then
        >> 1
        _else
        >> 2
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    // _if is the RHS of the assignment so it indents one tab; _then/_else/_endif align with _if;
    // bodies indent two tabs.
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB),
            insertAt(3, TAB),
            insertAt(4, TAB2),
            insertAt(5, TAB),
            insertAt(6, TAB2),
            insertAt(7, TAB));
  }

  @Test
  void testIndentAssignmentWithIfInline() {
    // When _if starts on the same line as the assignment (a << _if x?), visual alignment
    // means _then/_endif align with the _if keyword (column 5 = "     "), and the body
    // is indented 1 tab further (column 5+8=13 = "\t     " with tabSize=8).
    final String code =
        """
        a << _if x?
        _then
        >> 10
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(insertAt(2, "     "), insertAt(3, "\t     "), insertAt(4, "     "));
  }

  @Test
  void testIndentMultipleAssignment() {
    final String code =
        """
        (a, b, c) <<
        (1, 2, 3)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  // ==========================================================================
  // Return statements
  // ==========================================================================

  @Test
  void testIndentReturnTuple() {
    final String code =
        """
        _return (
        1,
        2
        )
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  @Test
  void testIndentReturnContinuation() {
    final String code =
        """
        _method a.b
        >>
        computed_value
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB2));
  }

  // ==========================================================================
  // Binary expression precedence
  // ==========================================================================

  @Test
  void testIndentAdditionChain() {
    // "result << " = 10 chars, so a is at column 10.
    // b and c should align with a: column 10 = "\t  " (1 tab + 2 spaces with tabSize=8)
    final String code =
        """
        result << a +
        b +
        c
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, "\t  "), insertAt(3, "\t  "));
  }

  @Test
  void testIndentMultiplicationChain() {
    // "result << " = 10 chars, so a is at column 10.
    // b and c should align with a: column 10 = "\t  " (1 tab + 2 spaces with tabSize=8)
    final String code =
        """
        result << a *
        b *
        c
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, "\t  "), insertAt(3, "\t  "));
  }

  // ==========================================================================
  // Comments
  // ==========================================================================

  @Test
  void testIndentCommentInArgumentList() {
    final String code =
        """
        result << method(
        arg1,
        # This is a comment about arg2
        arg2
        )
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB), insertAt(4, TAB));
  }

  @Test
  void testIndentCommentInMethodBody() {
    final String code =
        """
        _method my_class.my_method()
        # Comment inside method body
        _if condition
        _then
        # Comment inside if body
        do_something()
        _endif
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB),
            insertAt(3, TAB),
            insertAt(4, TAB),
            insertAt(5, TAB2),
            insertAt(6, TAB2),
            insertAt(7, TAB));
  }

  // ==========================================================================
  // Handling statement
  // ==========================================================================

  @Test
  void testIndentHandlingWithProc() {
    final String code =
        """
        _method a.b
        _handling warning, error _with
        _proc(cond)
        write(cond)
        _endproc
        _block
        do_something()
        _endblock
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB), // _handling
            insertAt(3, TAB2), // _proc
            insertAt(4, TAB3), // write(cond)
            insertAt(5, TAB2), // _endproc
            insertAt(6, TAB), // _block
            insertAt(7, TAB2), // do_something()
            insertAt(8, TAB)); // _endblock
  }

  // ==========================================================================
  // Import statement
  // ==========================================================================

  @Test
  void testIndentImportStatement() {
    final String code =
        """
        _proc()
        _import outer_var
        outer_var.do()
        _endproc
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  // ==========================================================================
  // Leave and continue
  // ==========================================================================

  @Test
  void testIndentLeaveWithLabel() {
    final String code =
        """
        _loop @outer
        _loop
        _leave @outer
        _endloop
        _endloop
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB2), insertAt(4, TAB));
  }

  // ==========================================================================
  // Slot assignment (additional)
  // ==========================================================================

  @Test
  void testIndentSlotAssignment() {
    final String code =
        """
        _method a.b
        .my_slot << compute_value()
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentSlotAssignmentContinuation() {
    final String code =
        """
        _method a.b
        .my_slot <<
        compute_value()
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB2));
  }

  @Test
  void testIndentAugmentedAssignment() {
    final String code =
        """
        _method a.b
        .counter +<< 1
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  // ==========================================================================
  // Deeply nested constructs
  // ==========================================================================

  @Test
  void testIndentDeeplyNested() {
    final String code =
        """
        _method my_class.complex_method()
        _try
        _if condition
        _then
        _for i _over range.elements()
        _loop
        do_something(i)
        _endloop
        _endif
        _when error
        handle_error()
        _endtry
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB), // _try
            insertAt(3, TAB2), // _if
            insertAt(4, TAB2), // _then
            insertAt(5, TAB3), // _for
            insertAt(6, TAB3), // _loop
            insertAt(7, TAB4), // do_something
            insertAt(8, TAB3), // _endloop
            insertAt(9, TAB2), // _endif
            insertAt(10, TAB), // _when
            insertAt(11, TAB2), // handle_error
            insertAt(12, TAB)); // _endtry
  }

  @Test
  void testIndentComplexMethodWithMultipleBlocks() {
    final String code =
        """
        _method my_class.process_data()
        _protect _locking _self
        _try
        _for item _over _self.items.elements()
        _loop
        _if item.valid?
        _then
        do_something(item)
        _endif
        _endloop
        _when error
        handle(error)
        _endtry
        _protection
        cleanup()
        _endprotect
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB), // _protect
            insertAt(3, TAB2), // _try
            insertAt(4, TAB3), // _for
            insertAt(5, TAB3), // _loop
            insertAt(6, TAB4), // _if
            insertAt(7, TAB4), // _then
            insertAt(8, TAB5), // do_something
            insertAt(9, TAB4), // _endif
            insertAt(10, TAB3), // _endloop
            insertAt(11, TAB2), // _when
            insertAt(12, TAB3), // handle
            insertAt(13, TAB2), // _endtry
            insertAt(14, TAB), // _protection
            insertAt(15, TAB2), // cleanup
            insertAt(16, TAB)); // _endprotect
  }

  // ==========================================================================
  // Protect with locking
  // ==========================================================================

  @Test
  void testIndentProtectLocking() {
    final String code =
        """
        _protect _locking _self
        do_something()
        _protection
        cleanup()
        _endprotect
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(4, TAB));
  }

  // ==========================================================================
  // Iter method / loopbody
  // ==========================================================================

  @Test
  void testIndentIterMethod() {
    final String code =
        """
        _iter _method my_class.elements()
        _for i _over _self.internal.elements()
        _loop
        _loopbody(i)
        _endloop
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB), // _for
            insertAt(3, TAB), // _loop
            insertAt(4, TAB2), // _loopbody
            insertAt(5, TAB)); // _endloop
  }

  @Test
  void testIndentLoopbody() {
    final String code =
        """
        _iter _method a.elements()
        _for item _over _self.items.elements()
        _loop
        _if item.valid?
        _then
        _loopbody(item)
        _endif
        _endloop
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(
            insertAt(2, TAB), // _for
            insertAt(3, TAB), // _loop
            insertAt(4, TAB2), // _if
            insertAt(5, TAB2), // _then
            insertAt(6, TAB3), // _loopbody
            insertAt(7, TAB2), // _endif
            insertAt(8, TAB)); // _endloop
  }

  // ==========================================================================
  // Scatter / global / dynamic references
  // ==========================================================================

  @Test
  void testIndentScatter() {
    final String code =
        """
        (_scatter
        my_collection)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, " "));
  }

  @Test
  void testIndentGlobalRef() {
    final String code =
        """
        _method a.b
        >> @my_global
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testIndentDynamicRef() {
    final String code =
        """
        _method a.b
        _dynamic !my_dynamic! << value
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  // ==========================================================================
  // Throw statement
  // ==========================================================================

  @Test
  void testIndentThrowStatement() {
    final String code =
        """
        _block
        _throw :error _with details
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  // ==========================================================================
  // Preserve tests - verify correctly formatted code is NOT changed
  // ==========================================================================

  @Test
  void testPreserveCorrectlyIndentedBlock() {
    final String code =
        """
        _block
        \tprint(1)
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveCorrectlyIndentedMethod() {
    final String code =
        """
        _method a.b(a, b, c)
        \tprint(1) # test method
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveCorrectlyIndentedIfElse() {
    final String code =
        """
        _if a
        _then
        \tshow(:a)
        _elif b
        _then
        \tshow(:b)
        _else
        \tshow(:c)
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveCorrectlyIndentedForLoop() {
    final String code =
        """
        _for i _over range.elements()
        _loop
        \tdo_something(i)
        _endloop
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveCorrectlyIndentedTryWhen() {
    final String code =
        """
        _try
        \tdo_something()
        _when error
        \thandle_error()
        _endtry
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveCorrectlyIndentedArguments() {
    final String code =
        """
        a.method(
        \tx,
        \ty,
        \tz)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveCorrectlyIndentedVector() {
    final String code =
        """
        _local a << {
        \t10
        }
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveCorrectlyIndentedMethodChain() {
    final String code =
        """
        obj.
        \tmethod1().
        \tmethod2().
        \tmethod3()
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveCorrectlyIndentedNestedConstruct() {
    final String code =
        """
        _method a.b
        \t_if a?
        \t_then
        \t\tdo_something()
        \t_endif
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  // ==========================================================================
  // Fix tests - verify incorrectly formatted code is corrected
  // ==========================================================================

  @Test
  void testFixOverIndentedBlock() {
    final String code =
        """
        _block
        \t\tprint(1)
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits)
        .containsExactly(new TextEdit(new Range(new Position(2, 1), new Position(2, 2)), ""));
  }

  @Test
  void testFixMixedWhitespace() {
    final String code =
        """
        _block
         \tprint(a)
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(replaceAt(2, 1, ""));
  }

  @Test
  void testFixBlankLineWithIndent() {
    final String code = "_method a.b\n\t\n\tdo_something()\n_endmethod\n";
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(replaceAt(2, 1, ""));
  }

  @Test
  void testFixUnindentedMethodArguments() {
    final String code =
        """
        a << obj.method(
        :symbol_arg,
        :another_symbol)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB), insertAt(3, TAB));
  }

  // ==========================================================================
  // Edge cases
  // ==========================================================================

  @Test
  void testPreserveInlineComment() {
    final String code =
        """
        _method a.b(a, b, c)
        \tprint(1) # test method
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveBlankLinesNoIndent() {
    final String code =
        """
        _method a.b

        \tdo_something()

        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveSameLineIfThen() {
    final String code =
        """
        _if a _then show(:a)
        _elif b _then show(:b)
        _else show(:c)
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveUnaryExpressions() {
    final String code =
        """
        _if _not condition?
        _then
        \tdo_something()
        _endif
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveEqualityExpression() {
    final String code =
        """
        _if a _is b
        _then
        \tdo_something()
        _endif
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
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).containsExactly(insertAt(2, TAB));
  }

  @Test
  void testPreservePackageStatement() {
    final String code =
        """
        _package my_package
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveMultiValueAssignment() {
    final String code =
        """
        (a, b, c) << (1, 2, 3)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveArgumentsVisuallyAlignedOnSameLine() {
    final String code =
        """
        a.method1(x,
        \t  y,
        \t  z)
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveParametersVisuallyAlignedOnSameLine() {
    final String code =
        """
        _method a.b(p_first,
        \t    p_second,
        \t    p_third)
        \tdo_something()
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreservePragmaAtTopLevel() {
    final String code =
        """
        _pragma(classify_level=basic)
        _method a.b
        \tdo_something()
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  // ==========================================================================
  // Empty constructs
  // ==========================================================================

  @Test
  void testPreserveEmptyMethod() {
    final String code =
        """
        _method a.b
        _endmethod
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveEmptyBlock() {
    final String code =
        """
        _block
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  @Test
  void testPreserveEmptyLoop() {
    final String code =
        """
        _for i _over range.elements()
        _loop
        _endloop
        """;
    final List<TextEdit> edits = this.getEdits(code);
    assertThat(edits).isEmpty();
  }

  // ==========================================================================
  // Options tests
  // ==========================================================================

  @Test
  void testIndentWithSpaces() {
    final FormattingOptions options = new FormattingOptions(4, true, false, true, false);
    final String code =
        """
        _block
        print(1)
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).containsExactly(insertAt(2, "    "));
  }

  @Test
  void testIndentWithTwoSpaces() {
    final FormattingOptions options = new FormattingOptions(2, true, false, true, false);
    final String code =
        """
        _block
        print(1)
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).containsExactly(insertAt(2, "  "));
  }

  @Test
  void testPreserveWithSpacesWhenCorrect() {
    final FormattingOptions options = new FormattingOptions(4, true, false, true, false);
    final String code =
        """
        _block
            print(1)
        _endblock
        """;
    final List<TextEdit> edits = this.getEdits(code, options);
    assertThat(edits).isEmpty();
  }
}
