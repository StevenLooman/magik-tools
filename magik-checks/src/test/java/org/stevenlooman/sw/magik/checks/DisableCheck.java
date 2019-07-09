package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class DisableCheck extends MagikCheckTestBase {

  @Test
  public void testNoMLint() {
    MagikCheck check = new SizeZeroEmptyCheck();

    String code =
        "a.size = 0  # mlint: disable=size-zero-empty";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testNoMLintDouble() {
    MagikCheck check1 = new SizeZeroEmptyCheck();
    MagikCheck check2 = new LhsRhsComparatorEqualCheck();

    String code =
        "a.size = 0 _andif a.size = 0  # mlint: disable=size-zero-empty, lhs-rhs-comparator-equal";
    List<MagikIssue> issues1 = runCheck(code, check1);
    assertThat(issues1).isEmpty();
    List<MagikIssue> issues2 = runCheck(code, check2);
    assertThat(issues2).isEmpty();
  }

  @Test
  public void testYesMLint() {
    MagikCheck check = new SizeZeroEmptyCheck();

    String code =
        "a.size = 0";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

}
