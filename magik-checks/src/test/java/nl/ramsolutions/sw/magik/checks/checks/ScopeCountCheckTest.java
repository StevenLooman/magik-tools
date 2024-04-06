package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** test ScopeCountCheck. */
@SuppressWarnings("checkstyle:MagicNumber")
class ScopeCountCheckTest extends MagikCheckTestBase {

  @Test
  void testTooManyScopeEntries() {
    final ScopeCountCheck check = new ScopeCountCheck();
    check.maxScopeCount = 1;
    final String code = """
        _method a.b
            _local l_a, l_b
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testOk() {
    final ScopeCountCheck check = new ScopeCountCheck();
    check.maxScopeCount = 10;
    final String code = """
        _method a.b
            _local l_a, l_b
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testTooManyScopeEntriesGlobals() {
    final ScopeCountCheck check = new ScopeCountCheck();
    check.maxScopeCount = 1;
    final String code = """
        _method a.b
            _global a, b
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
