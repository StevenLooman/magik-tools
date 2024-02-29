package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test MethodLineCountCheck. */
class MethodLineCountCheckTest extends MagikCheckTestBase {

  @Test
  void testMethodTooLong() {
    final MethodLineCountCheck check = new MethodLineCountCheck();
    check.maximumLineCount = 2;
    final String code =
        ""
            + "_method a.b\n"
            + "    _if a\n"
            + "    _then\n"
            + "      do()\n"
            + "    _endif\n"
            + "_endmethod\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMethodOk() {
    final MethodLineCountCheck check = new MethodLineCountCheck();
    check.maximumLineCount = 2;
    final String code = "" + "_method a.b\n" + "    do()\n" + "_endmethod\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testProcedureTooLong() {
    final MethodLineCountCheck check = new MethodLineCountCheck();
    check.maximumLineCount = 2;
    final String code =
        ""
            + "_proc()\n"
            + "    _if a\n"
            + "    _then\n"
            + "      do()\n"
            + "    _endif\n"
            + "_endproc\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testProcedureOk() {
    final MethodLineCountCheck check = new MethodLineCountCheck();
    check.maximumLineCount = 2;
    final String code = "" + "_proc()\n" + "    do()\n" + "_endproc\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testSyntaxError() {
    final MethodLineCountCheck check = new MethodLineCountCheck();
    final String code = "" + "_method a.b\n" + "    >> _self.\n" + "_endmethod";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
