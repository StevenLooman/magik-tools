package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test NoStatementAfterBodyExitCheck. */
class NoStatementAfterBodyExitCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
            _return 10
        _endmethod""",
        """
        _method a.b
            >> 10
        _endmethod""",
        """
        _method a.b
            _return 10
         \s\s\s
            # comment
        _endmethod""",
        """
        _loop
            _leave
        _endloop""",
      })
  void testValid(final String code) {
    final MagikCheck check = new NoStatementAfterBodyExitCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
            >> 10
            write(10)
        _endmethod""",
        """
        _loop
            _leave
            write(10)
        _endloop""",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new NoStatementAfterBodyExitCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
