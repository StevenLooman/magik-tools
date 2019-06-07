package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikCheck;

import java.util.List;

public class LineLengthCheckTest extends MagikCheckTestBase {

  @Test
  public void testLineNotTooLong_1() {
    MagikCheck check = new LineLengthCheck();

    String code =
        "# this is ok\n" +
        "print(a)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testLineNotTooLong_2() {
    MagikCheck check = new LineLengthCheck();

    String code =
        "l_23456789012345678901234567890" +
        "print(a)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testLineTooLong() {
    LineLengthCheck check = new LineLengthCheck();
    check.maxLineLength = 30;

    String code =
        "l_234567890123456789012345678901\n" +
        "print(a)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testLineTooLongComment() {
    LineLengthCheck check = new LineLengthCheck();
    check.maxLineLength = 30;

    String code =
        "# 234567890123456789012345678901\n" +
        "print(a)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

}
