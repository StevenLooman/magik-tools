package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

public class UndefinedVariableCheckTest extends MagikCheckTestBase {

  @Test
  public void testDefinedLocalUsed() {
    MagikCheck check = new UndefinedVariableCheck();
    String code =
        "_method a.b\n" +
        "\t_local l_a << 10\n" +
        "\twrite(l_a)\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testDefinedDefinitionUsed() {
    MagikCheck check = new UndefinedVariableCheck();
    String code =
        "_method a.b\n" +
        "\tl_a << 10\n" +
        "\twrite(l_a)\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testUndefinedLocalUsed() {
    MagikCheck check = new UndefinedVariableCheck();
    String code =
        "_method a.b\n" +
        "\twrite(l_a)\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testDefinedLocalUsedProcedure() {
    MagikCheck check = new UndefinedVariableCheck();
    String code =
        "_proc()\n" +
        "\t_local l_a\n" +
        "\twrite(l_a)\n" +
        "_endproc";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testUndefinedLocalUsedProcedure() {
    MagikCheck check = new UndefinedVariableCheck();
    String code =
        "_proc()\n" +
        "\twrite(l_a)\n" +
        "_endproc";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testParameter() {
    MagikCheck check = new UndefinedVariableCheck();
    String code =
    "_method a.b(p_a)\n" +
    "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testSerialAssigment() {
    MagikCheck check = new UndefinedVariableCheck();
    String code =
        "_method a.b()\n" +
        "  l_a << l_b << 10\n" +
        "  show(l_a, l_b)\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
 }

}