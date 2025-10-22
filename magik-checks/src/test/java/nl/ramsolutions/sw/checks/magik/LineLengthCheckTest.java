package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;

/** Test {@link LineLengthCheck}. */
@SuppressWarnings("checkstyle:MagicNumber")
class LineLengthCheckTest {

  @Test
  void testLineNotTooLong1() {
    final MagikCheck check = new LineLengthCheck();
    final String code =
        """
        # this is ok
        print(a)
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testLineNotTooLong2() {
    final MagikCheck check = new LineLengthCheck();
    final String code =
        """
        l_23456789012345678901234567890
        print(a)
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testLineTooLong() {
    final LineLengthCheck check = new LineLengthCheck();
    check.maxLineLength = 30;
    final String code =
        """
        l_234567890123456789012345678901
        print(a)
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testLineTooLongComment() {
    final LineLengthCheck check = new LineLengthCheck();
    check.maxLineLength = 30;
    final String code =
        """
        # 234567890123456789012345678901
        print(a)
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testExpandTab() {
    final LineLengthCheck check = new LineLengthCheck();
    check.maxLineLength = 40; // 5 tabs * 8 chars/tab = 40 chars
    final String code = "" + "\t\t\t\t\tprint(a)\n";
    assertThat(check).reportsIssueCount(code, 1);
  }
}
