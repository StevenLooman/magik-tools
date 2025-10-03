package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link NoStatementAfterBodyExitCheck}. */
class NoStatementAfterBodyExitCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _return 10
        _endmethod
        """,
        """
        _method a.b
          >> 10
        _endmethod
        """,
        """
        _method a.b
          _return 10
         \s\s\s
          # comment
        _endmethod
        """,
        """
        _loop
          _leave
        _endloop""",
      })
  void testValid(final String code) {
    final MagikCheck check = new NoStatementAfterBodyExitCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          >> 10
          write(10)
        _endmethod
        """,
        """
        _loop
          _leave
          write(10)
        _endloop""",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new NoStatementAfterBodyExitCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
