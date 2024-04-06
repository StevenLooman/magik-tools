package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test TrailingWhitespaceCheck. */
@SuppressWarnings("checkstyle:MagicNumber")
class TrailingWhitespaceCheckTest extends MagikCheckTestBase {

  @Test
  void testNoTrailingWhitespace() {
    final MagikCheck check = new TrailingWhitespaceCheck();
    final String code = "a";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testTrailingWhitespace() {
    final MagikCheck check = new TrailingWhitespaceCheck();
    final String code = "a    ";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
    final MagikIssue issue0 = issues.get(0);
    assertThat(issue0.location())
        .isEqualTo(
            new Location(
                URI.create("tests://unittest"), new Range(new Position(1, 1), new Position(1, 5))));
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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(3);
  }
}
