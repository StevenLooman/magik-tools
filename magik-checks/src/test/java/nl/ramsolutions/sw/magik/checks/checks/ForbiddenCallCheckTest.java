package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test ForbiddenCallCheck. */
class ForbiddenCallCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "do_something(1)",
        "sw:do_something(1)",
      })
  void testValid(final String code) {
    final MagikCheck check = new ForbiddenCallCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "show(1)",
        "sw:show(1)",
        "print(1)",
        "sw:print(1)",
        "debug_print(1)",
        "sw:debug_print(1)",
        "a.sys!perform(:|xyz()|, x, y, z)",
        "a.sys!perform_iter(:|xyz()|, x, y, z)",
        "a.sys!slot(:a)",
        "a.sys!slot(:a)<< _unset",
        "a.sys!slot(:a) << _unset",
        "a.sys!slot(:a) ^<< _unset"
      })
  void testInvalid(final String code) {
    final MagikCheck check = new ForbiddenCallCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMethodInvocation() {
    final String code = "a.forbidden_method()";
    final ForbiddenCallCheck check = new ForbiddenCallCheck();
    check.forbiddenCalls = ".forbidden_method()";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testOverridingForbiddenCallsClearsDefaultCalls() {
    final String code = "show(1)";
    final ForbiddenCallCheck check = new ForbiddenCallCheck();
    check.forbiddenCalls = ".forbidden_method()";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
