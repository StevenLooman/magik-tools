package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link TodoCommentCheck}. */
class TodoCommentCheckTest {

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
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "# TODO: This should trigger an issue",
        "# XXX: This should trigger an issue",
      })
  void testForbiddenWord(final String code) {
    final MagikCheck check = new TodoCommentCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "# XXX TODO: This should trigger two issues",
        "# XXX FIXME: This should trigger two issues",
      })
  void testForbiddenWordTwice(final String code) {
    final MagikCheck check = new TodoCommentCheck();
    assertThat(check).reportsIssueCount(code, 2);
  }
}
