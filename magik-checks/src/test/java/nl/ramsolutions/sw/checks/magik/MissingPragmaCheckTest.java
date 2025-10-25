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
        _method a.b _endmethod
        """,
        """
        a.define_shared_constant(:test_constant, 1, :private)
        """,
        """
        a.define_shared_variable(:test_constant, 1, :private)
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
