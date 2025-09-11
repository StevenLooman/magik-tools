package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link VariableCountCheck}. */
class VariableCountCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _local a << 10
          _local b << 20
          _local c << 30
          show(a, b, c)
        _endmethod
        """,
        """
        _proc()
          _local a << 10
          _local b << 20
          _local c << 30
          show(a, b, c)
        _endproc
        """,
      })
  void testInvalid(final String code) {
    final VariableCountCheck check = new VariableCountCheck();
    check.maxVariableCount = 2;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _local a << 10
          show(a)
        _endmethod
        """,
        """
        _proc()
          _local a << 10
          show(a)
        _endproc
        """,
        """
        _method a.
        _endmethod
        """,
      })
  void testValid(final String code) {
    final VariableCountCheck check = new VariableCountCheck();
    check.maxVariableCount = 2;
    assertThat(check).reportsNoIssues(code);
  }
}
