package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class CommentedCodeCheckTest extends MagikCheckTest {

  @Test
  public void testNoCommentedCode() {
    MagikCheck check = new CommentedCodeCheck();
    String code =
        "_method a.b\n" +
        "\t_local x << _self.call()\n" +
        "\tx +<< 10\n" +
        "\twrite(x)\n" +
        "\t_return x\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testCommentedCode() {
    MagikCheck check = new CommentedCodeCheck();
    String code =
        "_method a.b\n" +
        "\t#_local x << _self.call()\n" +
        "\t#x +<< 10\n" +
        "\t#write(x)\n" +
        "\t#_return x\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testCommentedCodeTwice() {
    MagikCheck check = new CommentedCodeCheck();
    String code =
        "_method a.b\n" +
            "\t#_local x << _self.call()\n" +
            "\t#x +<< 10\n" +
            "\t#write(x)\n" +
            "\t#_return x\n" +
            "\t_return 10\n" +
            "\t#_local x << _self.call()\n" +
            "\t#x +<< 10\n" +
            "\t#write(x)\n" +
            "\t#_return x\n" +
            "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(2);
  }


}
