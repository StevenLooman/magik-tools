package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test LineLengthCheck. */
@SuppressWarnings("checkstyle:MagicNumber")
class LineLengthCheckTest extends MagikCheckTestBase {

  @Test
  void testLineNotTooLong1() {
    final MagikCheck check = new LineLengthCheck();
    final String code = "" + "# this is ok\n" + "print(a)\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testLineNotTooLong2() {
    final MagikCheck check = new LineLengthCheck();
    final String code = "" + "l_23456789012345678901234567890\n" + "print(a)\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testLineTooLong() {
    final LineLengthCheck check = new LineLengthCheck();
    check.maxLineLength = 30;
    final String code = "" + "l_234567890123456789012345678901\n" + "print(a)\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testLineTooLongComment() {
    final LineLengthCheck check = new LineLengthCheck();
    check.maxLineLength = 30;
    final String code = "" + "# 234567890123456789012345678901\n" + "print(a)\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testExpandTab() {
    final LineLengthCheck check = new LineLengthCheck();
    check.maxLineLength = 40;
    final String code = "" + "\t\t\t\t\tprint(a)\n"; // 5 tabs * 8 chars/tab = 40 chars
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);

    final MagikIssue issue = issues.get(0);
    assertThat(issue.startColumn()).isEqualTo(5);
    assertThat(issue.endColumn()).isEqualTo(13);
    assertThat(issue.message()).isEqualTo("Line is too long (48/40)");
  }
}
