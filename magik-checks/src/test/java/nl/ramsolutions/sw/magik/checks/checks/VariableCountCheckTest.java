package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test VariableCountCheck. */
class VariableCountCheckTest extends MagikCheckTestBase {

  @Test
  void testMethodTooManyVariables() {
    final VariableCountCheck check = new VariableCountCheck();
    check.maximumVariableCount = 2;
    final String code =
        """
        _method a.b
            _local a << 10
            _local b << 20
            _local c << 30
            show(a, b, c)
        _endmethod
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMethodOk() {
    final VariableCountCheck check = new VariableCountCheck();
    check.maximumVariableCount = 2;
    final String code =
        """
        _method a.b
            _local a << 10
            show(a)
        _endmethod
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testProcedureTooManyVariables() {
    final VariableCountCheck check = new VariableCountCheck();
    check.maximumVariableCount = 2;
    final String code =
        """
        _proc()
            _local a << 10
            _local b << 20
            _local c << 30
            show(a, b, c)
        _endproc
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testProcedureOk() {
    final VariableCountCheck check = new VariableCountCheck();
    check.maximumVariableCount = 2;
    final String code =
        """
        _proc()
            _local a << 10
            show(a)
        _endproc
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testMethodSyntaxError() {
    final VariableCountCheck check = new VariableCountCheck();
    check.maximumVariableCount = 2;
    final String code = """
        _method a.
        _endmethod
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
