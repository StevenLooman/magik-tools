package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test WarnedCallCheck. */
class WarnedCallCheckTest extends MagikCheckTestBase {

  @Test
  void testOk() {
    final MagikCheck check = new WarnedCallCheck();
    final String code = "do_something(1)";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testProcedureShow() {
    final MagikCheck check = new WarnedCallCheck();
    final String code = "show(1)";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testProcedureWrite() {
    final MagikCheck check = new WarnedCallCheck();
    final String code = "write(1)";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }
}
