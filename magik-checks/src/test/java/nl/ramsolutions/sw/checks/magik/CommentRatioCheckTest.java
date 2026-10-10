package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;

/** Test {@link CommentRatioCheck}. */
class CommentRatioCheckTest {

  @Test
  void testHighRatio() {
    // 2 comment lines, 3 code lines: 2 / (3 + 2) = 40%.
    final String code =
        """
        _method a.b()
          ## This is a comment
          _return 1  # inline comment
        _endmethod
        """;
    final MagikCheck check = new CommentRatioCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testLineWithCodeAndCommentCountsAsBoth() {
    // 1 comment line, 3 code lines: 1 / (3 + 1) = 25%, not 1 / 3 = 33%.
    final String code =
        """
        _method a.b()
          _return 1  # inline comment
        _endmethod
        """;
    final CommentRatioCheck check = new CommentRatioCheck();
    check.minCommentPercentage = 25;
    assertThat(check).reportsNoIssues(code);
    check.minCommentPercentage = 26;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testHeaderCommentNotCounted() {
    // Header comment is ignored, just like in the SonarQube metrics: 0 / 3 = 0%.
    final String code =
        """
        # Header comment 1
        # Header comment 2
        # Header comment 3
        _method a.b()
          _return 1
        _endmethod
        """;
    final MagikCheck check = new CommentRatioCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testLowRatio() {
    final String code =
        """
        _method a.b()
          _local x << 1
          _local y << 2
          _local z << 3
          _local w << 4
          _return x + y + z + w
        _endmethod
        """;
    final MagikCheck check = new CommentRatioCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testZeroPercentage() {
    final String code =
        """
        _method a.b()
          _return 1
        _endmethod
        """;
    final CommentRatioCheck check = new CommentRatioCheck();
    check.minCommentPercentage = 0;
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testBlankLinesNotCounted() {
    // 2 comment lines, 5 code lines, 4 blank lines: 2 / (5 + 2) = 28%.
    final String code =
        """
        _method a.b()

          # Comment 1
          _local x << 1

          # Comment 2
          _local y << 2

          _return x + y

        _endmethod
        """;
    final CommentRatioCheck check = new CommentRatioCheck();
    check.minCommentPercentage = 28;
    assertThat(check).reportsNoIssues(code);
    check.minCommentPercentage = 29;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testTrailingCommentsCounted() {
    // 2 comment lines, 3 code lines: 2 / (3 + 2) = 40%.
    final String code =
        """
        _method a.b()
          _return 1
        _endmethod
        # Trailing comment 1
        # Trailing comment 2
        """;
    final CommentRatioCheck check = new CommentRatioCheck();
    check.minCommentPercentage = 40;
    assertThat(check).reportsNoIssues(code);
    check.minCommentPercentage = 41;
    assertThat(check).reportsIssueCount(code, 1);
  }
}
