package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test UndefinedVariableCheck. */
class UndefinedVariableCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
    _method a.b
        _local l_a << 10
        write(l_a)
    _endmethod
    """,
        """
    _method a.b
        l_a << 10
        write(l_a)
    _endmethod
    """,
        """
    _proc()
        _local l_a
        write(l_a)
    _endproc
    """,
        """
    _method a.b(p_a)
    _endmethod
    """,
        """
    _method a.b()
        l_a << l_b << 10
        show(l_a, l_b)
    _endmethod
    """,
      })
  void testValid(final String code) {
    final MagikCheck check = new UndefinedVariableCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
    _method a.b
        write(l_a)
    _endmethod
    """,
        """
    _proc()
        write(l_a)
    _endproc
    """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new UndefinedVariableCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
