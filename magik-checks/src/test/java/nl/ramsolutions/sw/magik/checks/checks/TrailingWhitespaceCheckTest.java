package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.api.Test;

/** Test {@link TrailingWhitespaceCheck}. */
@SuppressWarnings("checkstyle:MagicNumber")
class TrailingWhitespaceCheckTest {

  @Test
  void testNoTrailingWhitespace() {
    final MagikCheck check = new TrailingWhitespaceCheck();
    final String code = "a";
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testTrailingWhitespace() {
    final MagikCheck check = new TrailingWhitespaceCheck();
    final String code = "a    ";
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testTrailingWhitespaceMultiline() {
    final MagikCheck check = new TrailingWhitespaceCheck();
    final String code =
        """
        a()\s\s\s\s
         \s\s\s\s\s\s\s\s
        b()\s\s\s\s
        """;
    assertThat(check).reportsIssueCount(code, 3);
  }
}
