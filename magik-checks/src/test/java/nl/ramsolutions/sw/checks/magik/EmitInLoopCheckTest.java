package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link EmitInLoopCheck}. */
class EmitInLoopCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          >> 10
        _endmethod
        """,
        """
        _method a.b
          _loop
            _if _true _then _leave _endif
          _finally
            >> 10
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _over 1.upto(3)
          _loop
          _finally
            >> 10
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _for i _over 1.upto(3)
          _loop
          _finally
            >> 10
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _over 1.upto(3)
          _loop
            _proc() >> 42 _endproc
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _loop
            _if _true
            _then
              >> 10
            _endif
            _leave
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _loop
            _try
              >> 10
            _when error
            _endtry
            _leave
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _loop
            _protect
              >> 10
            _protection
            _endprotect
            _leave
          _endloop
        _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new EmitInLoopCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _loop
            >> 10
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _over 1.upto(3)
          _loop
            >> 10
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _for i _over 1.upto(3)
          _loop
            >> 10
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _local done? << _false
          _while _not done?
          _loop
            done? << _true
            >> 10
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _loop
            _loop
              >> 10
            _endloop
            _leave
          _endloop
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new EmitInLoopCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testInvalidMultipleEmits() {
    final String code =
        """
        _method a.b
          _loop
            >> 10
            >> 20
          _endloop
        _endmethod
        """;
    final MagikCheck check = new EmitInLoopCheck();
    assertThat(check).reportsIssueCount(code, 2);
  }
}
