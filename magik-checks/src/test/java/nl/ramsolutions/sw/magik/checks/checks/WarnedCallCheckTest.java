package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test WarnedCallCheck. */
class WarnedCallCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "do_something(1)",
        "show(1)",
      })
  void testValid(final String code) {
    final MagikCheck check = new WarnedCallCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
