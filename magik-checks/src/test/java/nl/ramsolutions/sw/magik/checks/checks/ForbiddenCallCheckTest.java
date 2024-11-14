package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
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
      strings = {"show(1)", "sw:show(1)", "a.sys!slot(:a)", "a.sys!perform(:|xyz()|, x, y, z)"})
  void testInvalid(final String code) {
    final MagikCheck check = new ForbiddenCallCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
