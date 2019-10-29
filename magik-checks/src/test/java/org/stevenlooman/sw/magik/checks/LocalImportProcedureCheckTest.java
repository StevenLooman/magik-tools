package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class LocalImportProcedureCheckTest extends MagikCheckTestBase {

  @Test
  public void testImportOk() {
    MagikCheck check = new LocalImportProcedureCheck();
    String code =
        "_method a.a\n" +
        "  _local x\n" +
        "  _proc()\n" +
        "    _import x\n" +
        "    x.do()\n" +
        "  _endproc\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testLocalButMeantImport() {
    MagikCheck check = new LocalImportProcedureCheck();
    String code =
        "_method a.a\n" +
        "  _local x\n" +
        "  _proc()\n" +
        "    _local x\n" +
        "    x.do()\n" +
        "  _endproc\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  public void testMethodProcedureParameter() {
    MagikCheck check = new LocalImportProcedureCheck();
    String code =
        "_method a.a(p_a)\n" +
        "  _proc(p_a)\n" +
        "  _endproc\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testTry() {
    MagikCheck check = new LocalImportProcedureCheck();
    String code =
        "_try _with a\n" +
        "_when error\n" +
        "_endtry\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

}
