package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link MissingPragmaCheck}. */
class MissingPragmaCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _pragma(classify_level=basic,topic={test},usage=subclassable)
        def_slotted_exemplar(:test_exemplar, {})
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=subclassable)
        _method a.b _endmethod
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=subclassable)
        a.define_shared_constant(:test_constant, 1, :private)
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=subclassable)
        a.define_shared_variable(:test_constant, 1, :private)
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=redefinable)
        _global prc << _proc() _endproc
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=redefinable)
        condition.define_condition(:cond, :information, {:data})
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new MissingPragmaCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        def_slotted_exemplar(:test_exemplar, {})
        """,
        """
        def_indexed_exemplar(:test_exemplar, {})
        """,
        """
        def_mixin(:test_mixin)
        """,
        """
        def_enumeration(:test_enum, _false, :a, :b)
        """,
        """
        _method a.b _endmethod
        """,
        """
        a.define_shared_constant(:test_constant, 1, :private)
        """,
        """
        a.define_shared_variable(:test_variable, 1, :private)
        """,
        """
        my_mixin.add_child(my_child)
        """,
        """
        _global prc << _proc() _endproc
        """,
        """
        condition.define_condition(:cond, :information, {:data})
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new MissingPragmaCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _pragma(classify_level=basic,topic={test},usage=subclassable)
        _method a.m
          tree.add_child(node)
        _endmethod
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=subclassable)
        _method a.m
          obj.define_shared_constant(:k, 1, :private)
        _endmethod
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=subclassable)
        _method a.m
          _block
            tree.add_child(node)
          _endblock
        _endmethod
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=subclassable)
        _block
          a_mixin.add_child(b)
        _endblock
        """,
        """
        _block
          a_mixin.add_child(b)
        _endblock
        """,
      })
  void testNestedInvocationsNotFlagged(final String code) {
    final MagikCheck check = new MissingPragmaCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _block
          def_slotted_exemplar(:example, {})
        _endblock
        """,
        """
        _block
          condition.define_condition(:cond, :information, {:data})
        _endblock
        """,
      })
  void testNonTopLevelDefinitionsNotFlagged(final String code) {
    final MagikCheck check = new MissingPragmaCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testGlobalDefinitionInBlock() {
    final String code =
        """
        _block
          _global prc
          prc << _proc() _endproc
        _endblock
        """;
    final MagikCheck check = new MissingPragmaCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testExemplarSlotAccessor() {
    final String code =
        """
        def_slotted_exemplar(:test_exemplar, {{:slot1, _unset, :readable, :public}})
        """;
    final MagikCheck check = new MissingPragmaCheck();
    assertThat(check).reportsIssueCount(code, 2);
  }
}
