package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Tests for ParameterCountCheck. */
class ParameterCountCheckTest extends MagikCheckTestBase {

  @Test
  void testMaxMethodParameterCountExceeded() {
    final ParameterCountCheck check = new ParameterCountCheck();
    check.maxParameterCount = 2;
    final String code =
        """
        _method object.x(p1, p2, p3)
        _endmethod
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
