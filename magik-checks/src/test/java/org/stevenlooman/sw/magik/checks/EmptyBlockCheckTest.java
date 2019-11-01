package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

public class EmptyBlockCheckTest extends MagikCheckTestBase {

  @Test
  public void testBlock() {
    MagikCheck check = new EmptyBlockCheck();

    String code =
        "_block\n" +
        "  write(a)\n" +
        "_endblock";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testBlockEmpty() {
    MagikCheck check = new EmptyBlockCheck();

    String code =
        "_block\n" +
        "_endblock";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testIfBlock() {
    MagikCheck check = new EmptyBlockCheck();

    String code =
        "_if a\n" +
        "_then\n" +
        "  write(a)\n" +
        "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testIfBlockEmpty() {
    MagikCheck check = new EmptyBlockCheck();

    String code =
        "_if a\n" +
        "_then\n" +
        "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testIfElifBlock() {
    MagikCheck check = new EmptyBlockCheck();

    String code =
        "_if a\n" +
        "_then\n" +
        "  write(a)\n" +
        "_elif b\n" +
        "_then\n" +
        "  write(b)\n" +
        "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testIfElifBlockEmpty() {
    MagikCheck check = new EmptyBlockCheck();

    String code =
        "_if a\n" +
        "_then\n" +
        "  write(a)\n" +
        "_elif b\n" +
        "_then\n" +
        "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testIfElseBlock() {
    MagikCheck check = new EmptyBlockCheck();

    String code =
        "_if a\n" +
        "_then\n" +
        "  write(a)\n" +
        "_else\n" +
        "  write(b)\n" +
        "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testIfElseBlockEmpty() {
    MagikCheck check = new EmptyBlockCheck();

    String code =
        "_if a\n" +
        "_then\n" +
        "  write(a)\n" +
        "_else\n" +
        "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testProtectEmpty() {
    MagikCheck check = new EmptyBlockCheck();

    String code =
        "_protect\n" +
        "_protection\n" +
        "  write(a)\n" +
        "_endprotect";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testProtectionEmpty() {
    MagikCheck check = new EmptyBlockCheck();

    String code =
        "_protect\n" +
        "  write(a)\n" +
        "_protection\n" +
        "_endprotect";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

}