package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link TodoCommentCheck}. */
class TodoCommentCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "# This should not trigger an issue",
        "# This should not trigger an issue, although it is used as a hack.",
        "# TEMPLATE: This should not trigger an issue",
        "# NOTES: This should not trigger an issue as well",
      })
  void testOk(final String code) {
    final MagikCheck check = new TodoCommentCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "# TODO: This should trigger an issue",
        "# XXX: This should trigger an issue",
      })
  void testForbiddenWord(final String code) {
    final MagikCheck check = new TodoCommentCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "# XXX TODO: This should trigger two issues",
        "# XXX FIXME: This should trigger two issues",
      })
  void testForbiddenWordTwice(final String code) {
    final MagikCheck check = new TodoCommentCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(2);
  }
}
