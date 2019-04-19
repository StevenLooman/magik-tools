package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikCheck;

import java.util.List;

public class SizeZeroEmptyCheckTest extends MagikCheckTest {

  @Test
  public void testSizeIsZero() {
    MagikCheck check = new SizeZeroEmptyCheck();
    String code =
        "a.size _is 0";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testSizeIsZeroReversed() {
    MagikCheck check = new SizeZeroEmptyCheck();
    String code =
        "0 _is a.size";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testSizeEqualsZero() {
    MagikCheck check = new SizeZeroEmptyCheck();
    String code =
        "a.size = 0";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testSizeEqualsZeroReversed() {
    MagikCheck check = new SizeZeroEmptyCheck();
    String code =
        "0 = a.size";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testEmpty() {
    MagikCheck check = new SizeZeroEmptyCheck();
    String code =
        "a.empty?\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

}
