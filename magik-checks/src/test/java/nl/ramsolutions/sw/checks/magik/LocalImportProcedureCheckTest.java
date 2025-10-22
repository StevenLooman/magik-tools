package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link LocalImportProcedureCheck}. */
class LocalImportProcedureCheckTest {

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
    assertThat(check).reportsNoIssues(code);
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
    assertThat(check).reportsIssueCount(code, 1);
  }
}
