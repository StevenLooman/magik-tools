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
        "" + "_method a.b\n" + "    _return 10\n" + "_endmethod",
        "" + "_method a.b\n" + "    >> 10\n" + "_endmethod",
        "" + "_method a.b\n" + "    _return 10\n" + "    \n" + "    # comment\n" + "_endmethod",
        "" + "_loop\n" + "    _leave\n" + "_endloop",
      })
  void testValid(final String code) {
    final MagikCheck check = new NoStatementAfterBodyExitCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "" + "_method a.b\n" + "    >> 10\n" + "    write(10)\n" + "_endmethod",
        "" + "_loop\n" + "    _leave\n" + "    write(10)\n" + "_endloop",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new NoStatementAfterBodyExitCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
