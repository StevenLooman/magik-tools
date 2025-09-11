package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.api.Test;

/** Test {@link MethodLineCountCheck}. */
class MethodLineCountCheckTest {

  @Test
  void testMethodTooLong() {
    final MethodLineCountCheck check = new MethodLineCountCheck();
    check.maxLineCount = 2;
    final String code =
        """
        _method a.b
          _if a
          _then
            do()
          _endif
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testMethodOk() {
    final MethodLineCountCheck check = new MethodLineCountCheck();
    check.maxLineCount = 2;
    final String code =
        """
        _method a.b
          do()
        _endmethod
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testProcedureTooLong() {
    final MethodLineCountCheck check = new MethodLineCountCheck();
    check.maxLineCount = 2;
    final String code =
        """
        _proc()
          _if a
          _then
            do()
          _endif
        _endproc
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testProcedureOk() {
    final MethodLineCountCheck check = new MethodLineCountCheck();
    check.maxLineCount = 2;
    final String code =
        """
        _proc()
          do()
        _endproc
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testSyntaxError() {
    final MagikCheck check = new MethodLineCountCheck();
    final String code =
        """
        _method a.b
          >> _self.
        _endmethod
        """;
    assertThat(check).reportsNoIssues(code);
  }
}
