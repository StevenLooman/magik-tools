package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikCheck;

import java.util.List;

public class UnusedVariableCheckTest extends MagikCheckTestBase {

  @Test
  public void testVariableUsed() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b\n" +
        "\t_local a\n" +
        "\tprint(a)\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testVariableNotUsed() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b\n" +
        "\t_local a\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testVariableNotUsedMultiAssignment() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b\n" +
        "\t_local (a, b) << (_scatter {1,2})\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testVariableFirstNotUsedMultiAssignment() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b\n" +
            "\t_local (a, b) << (_scatter {1,2})\n" +
            "\twrite(b)\n" +
            "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testDynamicAssigned() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b\n" +
        "\t_dynamic !notify_database_data_changes?! << _false\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testDynamicAssignedLater() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b\n" +
        "\t_dynamic !notify_database_data_changes?!\n" +
        "\t!notify_database_data_changes?! << _false\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testDynamicNotAssigned() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b\n" +
        "\t_dynamic !notify_database_data_changes?!\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testAssignmentUsed() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b\n" +
        "\ta << 10\n" +
        "\tprint(a)\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testAssignmentNotUsed() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b\n" +
        "\ta << 10\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testAssignmentMethod() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b(param1)\n" +
        "\tparam1.a << 10\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testParameter() {
    MagikCheck check = new UnusedVariableCheck(true);
    String code =
        "_method a.b(p_param1)\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testParameterOptional() {
    MagikCheck check = new UnusedVariableCheck(true);
    String code =
        "_method a.b(p_param1, _optional p_param2)\n" +
        "\twrite(p_param1)\n" +
        "\twrite(p_param2)\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testParameterGather() {
    MagikCheck check = new UnusedVariableCheck(true);
    String code =
        "_method a.b(p_param1, _gather p_param2)\n" +
            "\twrite(p_param1)\n" +
            "\twrite(p_param2)\n" +
            "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testMethodProcedureImport() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b\n" +
        "\t_local l_me << _self\n" +
        "\t_proc()\n" +
        "\t\t_import l_me\n" +
        "\t\tprint(l_me)\n" +
        "\t_endproc()\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testCase() {
    MagikCheck check = new UnusedVariableCheck();
    String code =
        "_method a.b()\n" +
        "\tl_Set << 10\n" +
        "\tprint(l_set)\n" +
        "_endmethod\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

}
