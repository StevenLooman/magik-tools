package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test LocalImportProcedureCheck. */
class LocalImportProcedureCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.a
            _local x
            _proc()
                _import x
                x.do()
            _endproc
        _endmethod
        """,
        """
        _method a.a(p_a)
            _proc(p_a)
            _endproc
        _endmethod
        """,
        """
        _try _with a
        _when error
        _endtry
        """,
        """
        _proc()
          _error
        _endproc
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new LocalImportProcedureCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.a
            _local x
            _proc()
                _local x
                x.do()
            _endproc
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new LocalImportProcedureCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
