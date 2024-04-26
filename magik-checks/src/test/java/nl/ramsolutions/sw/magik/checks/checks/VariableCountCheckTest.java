package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test VariableCountCheck. */
class VariableCountCheckTest extends MagikCheckTestBase {

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
    check.maximumVariableCount = 2;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
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
    check.maximumVariableCount = 2;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
