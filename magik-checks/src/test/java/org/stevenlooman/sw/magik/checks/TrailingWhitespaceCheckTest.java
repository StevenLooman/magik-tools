package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class TrailingWhitespaceCheckTest extends MagikCheckTest {

  @Test
  public void testNoTrailingWhitespace() {
    MagikCheck check = new TrailingWhitespaceCheck();
    String code =
        "a";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testTrailingWhitespace() {
    MagikCheck check = new TrailingWhitespaceCheck();
    String code =
        "a  ";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }


  @Test
  public void testTrailingWhitespaceMultiline() {
    MagikCheck check = new TrailingWhitespaceCheck();
    String code =
        "a()  \n" +
        "     \n" +
        "b()  \n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(3);
  }

}
