package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link WarnedCallCheck}. */
class WarnedCallCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "do_something(1)",
        "show(1)",
      })
  void testValid(final String code) {
    final MagikCheck check = new WarnedCallCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "write(1)",
        "sw:write(1)",
        "remex(:exemplar)",
        "sw:remex(:exemplar)",
        "remove_exemplar(:exemplar)",
        "sw:remove_exemplar(:exemplar)",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new WarnedCallCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testMethodInvocation() {
    final String code = "a.warned_method()";
    final WarnedCallCheck check = new WarnedCallCheck();
    check.warnedCalls = ".warned_method()";
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testOverridingWarnedCallsClearsDefaultCalls() {
    final String code = "write(1)";
    final WarnedCallCheck check = new WarnedCallCheck();
    check.warnedCalls = ".warned_method()";
    assertThat(check).reportsNoIssues(code);
  }
}
