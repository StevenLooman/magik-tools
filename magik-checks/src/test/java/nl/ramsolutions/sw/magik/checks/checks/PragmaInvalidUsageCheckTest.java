package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests {@link PragmaInvalidUsageCheck}. */
public class PragmaInvalidUsageCheckTest extends MagikCheckTestBase {

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
    final MagikCheck check = new PragmaInvalidUsageCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _pragma(classify_level=basic,topic={test},usage=invalid)
        def_slotted_exemplar(:test_exemplar, {})
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=invalid)
        _method a.b _endmethod
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=invalid)
        a.define_shared_constant(:test_constant, 1, :private)
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=invalid)
        a.define_shared_variable(:test_constant, 1, :private)
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=invalid)
        _global prc << _proc() _endproc
        """,
        """
        _pragma(classify_level=basic,topic={test},usage=invalid)
        condition.define_condition(:cond, :information, {:data})
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new PragmaInvalidUsageCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
