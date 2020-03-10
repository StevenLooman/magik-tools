package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class NoStatementAfterBodyExitCheckTest extends MagikCheckTestBase {

  @Test
  public void testNoStatementAfterReturn() {
    MagikCheck check = new NoStatementAfterBodyExitCheck();
    String code =
        "_method a.b\n" +
        "\t_return 10\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testStatementAfterReturn() {
    MagikCheck check = new NoStatementAfterBodyExitCheck();
    String code =
        "_method a.b\n" +
        "\t>> 10\n" +
        "\twrite(10)\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testNoStatementAfterReturnComment() {
    MagikCheck check = new NoStatementAfterBodyExitCheck();
    String code =
        "_method a.b\n" +
        "\t_return 10\n" +
        "\t\n" +
        "\t# comment\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testNoStatementAfterEmit() {
    MagikCheck check = new NoStatementAfterBodyExitCheck();
    String code =
        "_method a.b\n" +
        "\t>> 10\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testStatementAfterEmit() {
    MagikCheck check = new NoStatementAfterBodyExitCheck();
    String code =
        "_method a.b\n" +
        "\t>> 10\n" +
        "\twrite(10)\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testNoStatementAfterLeave() {
    MagikCheck check = new NoStatementAfterBodyExitCheck();
    String code =
        "_loop\n" +
        "\t_leave\n" +
        "_endloop";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testStatementAfterLeave() {
    MagikCheck check = new NoStatementAfterBodyExitCheck();
    String code =
        "_loop\n" +
        "\t_leave\n" +
        "\twrite(10)\n" +
        "_endloop";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

}
