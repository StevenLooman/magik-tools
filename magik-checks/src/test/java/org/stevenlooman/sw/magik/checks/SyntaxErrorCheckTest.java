package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class SyntaxErrorCheckTest extends MagikCheckTestBase {

  @Test
  public void testSyntaxError() {
    MagikCheck check = new SyntaxErrorCheck();
    String code =
        "_block\n" +
        "_endbloc";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testSytnaxError2() {
    MagikCheck check = new SyntaxErrorCheck();
    String code =
        "_method";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

}
