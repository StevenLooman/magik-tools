package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Test {@link ScopeCountCheck}. */
@SuppressWarnings("checkstyle:MagicNumber")
class ScopeCountCheckTest {

  @Test
  void testTooManyScopeEntries() {
    final ScopeCountCheck check = new ScopeCountCheck();
    check.maxScopeCount = 1;
    final String code =
        """
        _method a.b
          _local l_a, l_b
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testOk() {
    final ScopeCountCheck check = new ScopeCountCheck();
    check.maxScopeCount = 10;
    final String code =
        """
        _method a.b
          _local l_a, l_b
        _endmethod
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testTooManyScopeEntriesGlobals() {
    final ScopeCountCheck check = new ScopeCountCheck();
    check.maxScopeCount = 1;
    final String code =
        """
        _method a.b
          _global a, b
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }
}
