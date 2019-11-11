package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;

public class ScopeCountCheckTest extends MagikCheckTestBase {

  @Test
  public void testTooManyScopeEntries() {
    ScopeCountCheck check = new ScopeCountCheck();
    check.maxScopeCount = 1;
    String code =
        "_method a.b\n" +
        "\t_local l_a, l_b\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testOk() {
    ScopeCountCheck check = new ScopeCountCheck();
    check.maxScopeCount = 10;
    String code =
        "_method a.b\n" +
        "\t_local l_a, l_b\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testTooManyScopeEntriesGlobals() {
    ScopeCountCheck check = new ScopeCountCheck();
    check.maxScopeCount = 1;
    String code =
        "_method a.b\n" +
        "\t_global a, b\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

}