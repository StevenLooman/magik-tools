package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link LeaveInProtectionCheck}. */
class LeaveInProtectionCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _protect
            _leave _with 10
          _protection
          _endprotect
        _endmethod
        """,
        """
        _method a.b
          _protect
          _protection
            _loop _leave _endloop
          _endprotect
        _endmethod
        """,
        """
        _method a.b
          _protect
          _protection
            _local p << _proc() _leave _with 10 _endproc
          _endprotect
        _endmethod
        """,
        // _leave in _block inside _protection - exits the block, not the protection
        """
        _method a.b
          _protect
          _protection
            _block _leave _endblock
          _endprotect
        _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new LeaveInProtectionCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _protect
          _protection
            _leave
          _endprotect
        _endmethod
        """,
        """
        _method a.b
          _protect
          _protection
            _leave _with 10
          _endprotect
        _endmethod
        """,
        """
        _method a.b
          _protect
          _protection
            _if _true _then _leave _endif
          _endprotect
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new LeaveInProtectionCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
