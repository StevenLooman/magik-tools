package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link ImportMissingDefinitionCheck}. */
class ImportMissingDefinitionCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _local var
          _proc()
            _import var
          _endproc
        _endmethod
        """,
        """
        _method a.b
          _if _true
          _then
            _local var
            _proc()
              _import var
            _endproc
          _endif
        _endmethod
        """,
        """
        _method a.b
          _constant const
          _proc()
            _import const
          _endproc
        _endmethod
        """,
        """
        _method a.b(param)
          _proc()
            _import param
          _endproc
        _endmethod
        """,
        """
        _method a.b(param)
          _for i _over 1.upto(5)
          _loop
            _proc()
              _import i
            _endproc
          _endloop
        _endmethod
        """,
      })
  void testImportValid(final String code) {
    final MagikCheck check = new ImportMissingDefinitionCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _proc()
            _import var
          _endproc
        _endmethod
        """,
        """
        _method a.b
          var << 1
          _proc()
            _import var
          _endproc
        _endmethod
        """,
      })
  void testImportInvalid(final String code) {
    final MagikCheck check = new ImportMissingDefinitionCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
