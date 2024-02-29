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
        ""
            + "_method a.b\n"
            + "    _local a << 10\n"
            + "    _local b << 20\n"
            + "    _local c << 30\n"
            + "    show(a, b, c)\n"
            + "_endmethod\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMethodOk() {
    final VariableCountCheck check = new VariableCountCheck();
    check.maximumVariableCount = 2;
    final String code =
        "" + "_method a.b\n" + "    _local a << 10\n" + "    show(a)\n" + "_endmethod\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testProcedureTooManyVariables() {
    final VariableCountCheck check = new VariableCountCheck();
    check.maximumVariableCount = 2;
    final String code =
        ""
            + "_proc()\n"
            + "    _local a << 10\n"
            + "    _local b << 20\n"
            + "    _local c << 30\n"
            + "    show(a, b, c)\n"
            + "_endproc\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testProcedureOk() {
    final VariableCountCheck check = new VariableCountCheck();
    check.maximumVariableCount = 2;
    final String code = "" + "_proc()\n" + "    _local a << 10\n" + "    show(a)\n" + "_endproc\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testMethodSyntaxError() {
    final VariableCountCheck check = new VariableCountCheck();
    check.maximumVariableCount = 2;
    final String code = "" + "_method a.\n" + "_endmethod\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
