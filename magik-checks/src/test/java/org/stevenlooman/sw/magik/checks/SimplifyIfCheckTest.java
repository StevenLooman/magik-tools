package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class SimplifyIfCheckTest extends MagikCheckTestBase {

  @Test
  public void testSimplifyIfIf() {
    MagikCheck check = new SimplifyIfCheck();
    String code =
      "_if a\n" +
      "_then\n" +
      "\t_if b\n" +
      "\t_then\n" +
      "\t_endif\n" +
      "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testSimplifyIfElseIf() {
    MagikCheck check = new SimplifyIfCheck();
    String code =
      "_if a\n" +
      "_then\n" +
      "_else\n" +
      "\t_if b\n" +
      "\t_then\n" +
      "\t_endif\n" +
      "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testNoSimplifyIfIf() {
    MagikCheck check = new SimplifyIfCheck();
    String code =
        "_if a\n" +
        "_then\n" +
        "\t_if b\n" +
        "\t_then\n" +
        "\t_endif\n" +
        "\tc()\n" +
        "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testNoSimplifyIfIfElif() {
    MagikCheck check = new SimplifyIfCheck();
    String code =
        "_if a\n" +
        "_then\n" +
        "\t_if b\n" +
        "\t_then\n" +
        "\t_elif c\n" +
        "\t_then\n" +
        "\t_endif\n" +
        "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testNoSimplifyIfIfElse() {
    MagikCheck check = new SimplifyIfCheck();
    String code =
        "_if a\n" +
        "_then\n" +
        "\t_if b\n" +
        "\t_then\n" +
        "\t_else\n" +
        "\t_endif\n" +
        "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testNoSimplifyIfElseIf() {
    MagikCheck check = new SimplifyIfCheck();
    String code =
      "_if a\n" +
      "_then\n" +
      "_else\n" +
      "\t_if b\n" +
      "\t_then\n" +
      "\t_endif\n" +
      "\tc()\n" +
      "_endif";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

}
