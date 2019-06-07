package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikCheck;

import java.util.List;

public class LhsRhsComparatorCheckTest extends MagikCheckTestBase {

  @Test
  public void testOrLhsRhsUnequal() {
    MagikCheck check = new LhsRhsComparatorEqualCheck();
    String code =
        "a _or b\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testOrLhsRhsEqual() {
    MagikCheck check = new LhsRhsComparatorEqualCheck();
    String code =
        "a _or a\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testXorLhsRhsUnequal() {
    MagikCheck check = new LhsRhsComparatorEqualCheck();
    String code =
        "a _xor b\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testXorLhsRhsEqual() {
    MagikCheck check = new LhsRhsComparatorEqualCheck();
    String code =
        "a _xor a\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testAndLhsRhsUnequal() {
    MagikCheck check = new LhsRhsComparatorEqualCheck();
    String code =
        "a _and b\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testAndLhsRhsEqual() {
    MagikCheck check = new LhsRhsComparatorEqualCheck();
    String code =
        "a _and a\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testIsLhsRhsUnequal() {
    MagikCheck check = new LhsRhsComparatorEqualCheck();
    String code =
        "a _is b\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testIsLhsRhsEqual() {
    MagikCheck check = new LhsRhsComparatorEqualCheck();
    String code =
        "a _is a\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testGeLhsRhsUnequal() {
    MagikCheck check = new LhsRhsComparatorEqualCheck();
    String code =
        "a >= b\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testGeLhsRhsEqual() {
    MagikCheck check = new LhsRhsComparatorEqualCheck();
    String code =
        "a >= a\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

}
