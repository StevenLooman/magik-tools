package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link UnnamedProcedureCheck}. */
class UnnamedProcedureCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "_proc@name() _endproc",
        "_iter _proc@items() _loopbody(_unset) _endproc",
      })
  void testValid(final String code) {
    final MagikCheck check = new UnnamedProcedureCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "_proc() _endproc",
        "_iter _proc() _loopbody(_unset) _endproc",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new UnnamedProcedureCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
