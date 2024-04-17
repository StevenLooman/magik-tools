package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test LocalImportProcedureCheck. */
class LocalImportProcedureCheckTest extends MagikCheckTestBase {

  @Test
  void testImportOk() {
    final MagikCheck check = new LocalImportProcedureCheck();
    final String code =
        """
        _method a.a
            _local x
            _proc()
                _import x
                x.do()
            _endproc
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testLocalButMeantImport() {
    final MagikCheck check = new LocalImportProcedureCheck();
    final String code =
        """
        _method a.a
            _local x
            _proc()
                _local x
                x.do()
            _endproc
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMethodProcedureParameter() {
    final MagikCheck check = new LocalImportProcedureCheck();
    final String code =
        """
        _method a.a(p_a)
            _proc(p_a)
            _endproc
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testTry() {
    final MagikCheck check = new LocalImportProcedureCheck();
    final String code =
        """
        _try _with a
        _when error
        _endtry
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testSyntaxError() {
    final MagikCheck check = new LocalImportProcedureCheck();
    final String code =
        """
        _proc()
          _error
        _endproc
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
