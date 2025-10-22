package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import org.junit.jupiter.api.Test;

/** Tests for {@link ParameterCountCheck}. */
class ParameterCountCheckTest {

  @Test
  void testMaxMethodParameterCountExceeded() {
    final ParameterCountCheck check = new ParameterCountCheck();
    check.maxParameterCount = 2;
    final String code =
        """
        _method object.x(p1, p2, p3)
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testMaxProcedureParameterCountExceeded() {
    final ParameterCountCheck check = new ParameterCountCheck();
    check.maxParameterCount = 2;
    final String code =
        """
        _proc(p1, p2, p3)
        _endproc
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Test
  void testMaxMethodParameterCountSatisfied() {
    final ParameterCountCheck check = new ParameterCountCheck();
    check.maxParameterCount = 3;
    final String code =
        """
        _method object.x(p1, p2, p3)
        _endmethod
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Test
  void testMaxProcedureParameterCountSatisfied() {
    final ParameterCountCheck check = new ParameterCountCheck();
    check.maxParameterCount = 3;
    final String code =
        """
        _proc(p1, p2, p3)
        _endproc
        """;
    assertThat(check).reportsNoIssues(code);
  }
}
