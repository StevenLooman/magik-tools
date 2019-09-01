package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class WarnedCallCheckTest extends MagikCheckTestBase {

  @Test
  public void testOk() {
    MagikCheck check = new WarnedCallCheck();
    String code = "do_something(1)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testProcedureShow() {
    MagikCheck check = new WarnedCallCheck();
    String code = "show(1)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testProcedureWrite() {
    MagikCheck check = new WarnedCallCheck();
    String code = "write(1)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

}
