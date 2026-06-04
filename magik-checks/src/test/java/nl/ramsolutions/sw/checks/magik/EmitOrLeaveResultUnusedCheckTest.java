package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link EmitOrLeaveResultUnusedCheck}. */
class EmitOrLeaveResultUnusedCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          >> 10
        _endmethod
        """,
        """
        _proc()
          >> 10
        _endproc
        """,
        """
        _method a.b
          _loop
            >> 10
          _endloop
        _endmethod
        """,
        """
        _method a.b
          >> _if _true _then >> :a _endif
        _endmethod
        """,
        """
        _method a.b
          _local x << _if _true _then >> :a _endif
        _endmethod
        """,
        """
        _method a.b
          >> _if _true
             _then
               >> :a
             _endif
        _endmethod
        """,
        """
        _method a.b
          >> _over 1.upto(3)
             _loop
             _finally
               >> 10
             _endloop
        _endmethod
        """,
        """
        _method a.b
          xxx << _block
                   _leave _with 20
                 _endblock
        _endmethod
        """,
        """
        _method a.b
          _if _true
          _then
            _local p << _proc() >> 42 _endproc
          _endif
        _endmethod
        """,
        """
        _method a.b
          string +<< _if _true
                     _then
                       >> " w"
                     _endif.default(" b")
        _endmethod
        """,
        """
        _method object.result_called?(a?)
          _if a?
          _then
            >> :a
          _endif.default(:b)
        _endmethod
        """,
      })
  void testEmitValid(final String code) {
    final MagikCheck check = new EmitOrLeaveResultUnusedCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _if _true _then >> 10 _endif
        _endmethod
        """,
        """
        _method a.b
          _if _false _then _else >> 10 _endif
        _endmethod
        """,
        """
        _method a.b
          _if _false
          _then
          _elif _true
          _then
            >> 10
          _endif
        _endmethod
        """,
        """
        _method a.b
          _try
            >> 10
          _when error
          _endtry
        _endmethod
        """,
        """
        _method a.b
          _try
          _when error
            >> 10
          _endtry
        _endmethod
        """,
        """
        _method a.b
          _protect
            >> 10
          _protection
          _endprotect
        _endmethod
        """,
        """
        _method a.b
          _protect
          _protection
            >> 10
          _endprotect
        _endmethod
        """,
        """
        _method a.b
          _block
            >> 10
          _endblock
        _endmethod
        """,
        """
        _method a.b
          _catch :tag
            >> 10
          _endcatch
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
          _if _true
          _then
            _if _true _then >> 10 _endif
          _endif
        _endmethod
        """,
      })
  void testEmitInvalid(final String code) {
    final MagikCheck check = new EmitOrLeaveResultUnusedCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _loop _leave _endloop
        _endmethod
        """,
        """
        _method a.b
          _leave _with 10
        _endmethod
        """,
        """
        _method a.b
          _if _true _then _leave _with 10 _endif
        _endmethod
        """,
        """
        _method a.b
          _if _false _then _else _leave _with 10 _endif
        _endmethod
        """,
        """
        _method a.b
          _if _false
          _then
          _elif _true
          _then
            _leave _with 10
          _endif
        _endmethod
        """,
        """
        _method a.b
          _try
            _leave _with 10
          _when error
          _endtry
        _endmethod
        """,
        """
        _method a.b
          _try
          _when error
            _leave _with 10
          _endtry
        _endmethod
        """,
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
          _catch :tag
            _leave _with 10
          _endcatch
        _endmethod
        """,
        """
        _method a.b
          _lock _self
            _leave _with 10
          _endlock
        _endmethod
        """,
        """
        _method a.b
          >> _loop _leave _with 10 _endloop
        _endmethod
        """,
        """
        _method a.b
          >> _block _leave _with 10 _endblock
        _endmethod
        """,
        """
        _method a.b
          z << _loop
                 _if a?
                 _then
                   _leave _with 10
                 _endif
               _endloop
        _endmethod
        """,
      })
  void testLeaveWithValid(final String code) {
    final MagikCheck check = new EmitOrLeaveResultUnusedCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _loop _leave _with 10 _endloop
        _endmethod
        """,
        """
        _method a.b
          _block _leave _with 10 _endblock
        _endmethod
        """,
        """
        _method a.b
          _over 1.upto(3)
          _loop
            _leave _with 10
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _loop
            _if a?
            _then
              _leave _with 10
            _endif
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _over 1.upto(3)
          _loop
            _if a?
            _then
              _leave _with 10
            _endif
          _endloop
        _endmethod
        """,
      })
  void testLeaveWithInvalid(final String code) {
    final MagikCheck check = new EmitOrLeaveResultUnusedCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testMultipleEmitsInSameConstruct() {
    final String code =
        """
        _method a.b
          _if _true
          _then
            >> 10
            >> 20
          _endif
        _endmethod
        """;
    final MagikCheck check = new EmitOrLeaveResultUnusedCheck();
    assertThat(check).reportsIssueCount(code, 2);
  }
}
