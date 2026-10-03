package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;

/** Test {@link CommentRatioCheck}. */
class CommentRatioCheckTest {

  @Test
  void testHighRatio() {
    final String code =
        """
        # This is a comment
        # This is another comment
        _method a.b()
          _return 1  # inline comment
        _endmethod
        """;
    final MagikCheck check = new CommentRatioCheck();
    assertThat(check).reportsNoIssues(code);
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
    check.minimumCommentPercentage = 0;
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testBlankLinesNotCounted() {
    // 3 comment lines, 4 code lines, 3 blank lines: percentage 3/7 = 42% > 25%.
    final String code =
        """
        # Comment 1
        # Comment 2

        _method a.b()

          _local x << 1
          _local y << 2
          _return x + y

        _endmethod

        # Comment 3
        """;
    final MagikCheck check = new CommentRatioCheck();
    assertThat(check).reportsNoIssues(code);
  }
}
