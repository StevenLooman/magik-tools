package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikCheck;

import java.util.List;

public class MethodComplexityCheckTest extends MagikCheckTestBase {

  @Test
  public void testTooComplex() {
    MethodComplexityCheck check = new MethodComplexityCheck();
    check.maximumComplexity = 5;

    String code =
        "_method a.b\n" +
        "\t_if a" +
        "\t_then" +
        "\t\t_if b" +
        "\t\t_then" +
        "\t\t\t_if c" +
        "\t\t\t_then" +
        "\t\t\t\t_if d" +
        "\t\t\t\t_then" +
        "\t\t\t\t\t_if e" +
        "\t\t\t\t\t_then" +
        "\t\t\t\t\t_endif\n" +
        "\t\t\t\t_endif\n" +
        "\t\t\t_endif\n" +
        "\t\t_endif\n" +
        "\t_endif\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
  @Test
  public void testNotTooComplex() {
    MagikCheck check = new MethodComplexityCheck();
    String code =
        "_method a.b\n" +
        "\t_if a" +
        "\t_then" +
        "\t_endif\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

}
