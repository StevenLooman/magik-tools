package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests {@link PragmaInvalidClassifyLevelCheck}. */
public class PragmaInvalidClassifyLevelCheckTest {

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
    final MagikCheck check = new PragmaInvalidClassifyLevelCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _pragma(classify_level=invalid,topic={test},usage=subclassable)
        def_slotted_exemplar(:test_exemplar, {})
        """,
        """
        _pragma(classify_level=invalid,topic={test},usage=subclassable)
        _method a.b _endmethod
        """,
        """
        _pragma(classify_level=invalid,topic={test},usage=subclassable)
        a.define_shared_constant(:test_constant, 1, :private)
        """,
        """
        _pragma(classify_level=invalid,topic={test},usage=subclassable)
        a.define_shared_variable(:test_constant, 1, :private)
        """,
        """
        _pragma(classify_level=invalid,topic={test},usage=redefinable)
        _global prc << _proc() _endproc
        """,
        """
        _pragma(classify_level=invalid,topic={test},usage=redefinable)
        condition.define_condition(:cond, :information, {:data})
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new PragmaInvalidClassifyLevelCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
