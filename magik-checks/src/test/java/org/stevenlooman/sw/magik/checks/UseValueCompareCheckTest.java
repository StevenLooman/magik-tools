package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikCheck;

import java.util.List;

public class UseValueCompareCheckTest extends MagikCheckTestBase {

  @Test
  public void testNoLiteral() {
    MagikCheck check = new UseValueCompareCheck();
    String code =
        "a _is b";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testStringValueCompare() {
    MagikCheck check = new UseValueCompareCheck();
    String code =
        "a = \"b\"";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testStringInstanceCompareLeft() {
    MagikCheck check = new UseValueCompareCheck();
    String code =
        "\"a\" _is b";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testStringInstanceCompareRight() {
    MagikCheck check = new UseValueCompareCheck();
    String code =
        "a _is \"b\"";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testIntegerInstanceCompare() {
    MagikCheck check = new UseValueCompareCheck();
    String code =
        "a _is 1";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testBignumInstanceCompare() {
    MagikCheck check = new UseValueCompareCheck();
    String code =
        "\"a\" _is 536870912";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testBignumFloat() {
    MagikCheck check = new UseValueCompareCheck();
    String code =
        "\"a\" _is 0.0";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

}