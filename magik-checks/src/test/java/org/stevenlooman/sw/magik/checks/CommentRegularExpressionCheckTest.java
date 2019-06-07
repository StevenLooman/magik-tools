package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class CommentRegularExpressionCheckTest extends MagikCheckTestBase {

  @Test
  public void testMatch() {
    CommentRegularExpressionCheck check = new CommentRegularExpressionCheck();
    check.regularExpression = ".*match_me.*";

    String code =
        "# match_me\n" +
        "print(a)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testNoMatch() {
    CommentRegularExpressionCheck check = new CommentRegularExpressionCheck();
    check.regularExpression = ".*do_not_match_me.*";

    String code =
        "# match_me\n" +
        "print(a)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
