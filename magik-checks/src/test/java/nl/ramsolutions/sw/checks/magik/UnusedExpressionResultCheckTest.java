package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link UnusedExpressionResultCheck}. */
class UnusedExpressionResultCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        // _if: assigned
        """
        x << _if a _then >> 1 _else >> 2 _endif
        """,
        // _if: forwarded
        """
        >> _if a _then >> 1 _else >> 2 _endif
        """,
        // _if: returned
        """
        _return _if a _then >> 1 _else >> 2 _endif
        """,
        // _if: method argument
        """
        foo(_if a _then >> 1 _else >> 2 _endif)
        """,
        // _if: used as condition
        """
        _if _if a _then >> _true _else >> _false _endif
        _then write("yes")
        _endif
        """,
        // _if: no >> in any branch
        """
        _if a
        _then
            write("yes")
        _else
            write("no")
        _endif
        """,
        // _if: nested inner assigned
        """
        _if outer
        _then
            x << _if inner _then >> 1 _else >> 2 _endif
        _endif
        """,
        // _try: assigned
        """
        x << _try >> _true _when error >> _false _endtry
        """,
        // _try: method argument
        """
        foo(_try >> _true _when error >> _false _endtry)
        """,
        // _try: no >>
        """
        _try
            write("ok")
        _when error
            write("err")
        _endtry
        """,
        // _block: assigned
        """
        x << _block >> 1 _endblock
        """,
        // _block: method argument
        """
        foo(_block >> 1 _endblock)
        """,
        // _block: no >>
        """
        _block write("ok") _endblock
        """,
        // _loop: assigned
        """
        x << _loop >> value _endloop
        """,
        // _loop: no >>
        """
        _loop write("ok") _endloop
        """,
        // _protect: assigned
        """
        x << _protect >> _true _protection >> _false _endprotect
        """,
        // _catch: assigned
        """
        x << _catch :tag >> value _endcatch
        """,
        // _lock: assigned
        """
        x << _lock mutex >> value _endlock
        """,
        // _over: assigned
        """
        x << _over 1.upto(10) _loop >> i _endloop
        """,
        // _while: assigned
        """
        x << _while condition _loop >> value _endloop
        """,
        // _for: assigned
        """
        x << _for i _over 1.upto(10) _loop >> i _endloop
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new UnusedExpressionResultCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // _if: both branches emit, result discarded
        """
        _if _true
        _then
            >> _true
        _else
            >> _false
        _endif
        """,
        // _if: only _then emits
        """
        _if a
        _then
            >> 1
        _endif
        """,
        // _if: only _else emits
        """
        _if a
        _then
            write("a")
        _else
            >> 2
        _endif
        """,
        // _if: _elif emits
        """
        _if a
        _then
            write("a")
        _elif b
        _then
            >> 2
        _endif
        """,
        // _if: standalone inside _loop body
        """
        _loop
            _if a _then >> 1 _else >> 2 _endif
        _endloop
        """,
        // _if: standalone inside outer _if _then
        """
        _if outer
        _then
            _if inner _then >> 1 _else >> 2 _endif
        _endif
        """,
        // _try: both emit, result discarded
        """
        _try >> _true _when error >> _false _endtry
        """,
        // _try: only try-body emits
        """
        _try >> 1 _when error write("err") _endtry
        """,
        // _try: only when-body emits
        """
        _try write("ok") _when error >> _false _endtry
        """,
        // _block: result discarded
        """
        _block >> 1 _endblock
        """,
        // _loop: result discarded
        """
        _loop >> value _endloop
        """,
        // _protect: result discarded
        """
        _protect >> _true _protection >> _false _endprotect
        """,
        // _catch: result discarded
        """
        _catch :tag >> value _endcatch
        """,
        // _lock: result discarded
        """
        _lock mutex >> value _endlock
        """,
        // _over: result discarded
        """
        _over 1.upto(10) _loop >> i _endloop
        """,
        // _while: result discarded
        """
        _while condition _loop >> value _endloop
        """,
        // _for: result discarded
        """
        _for i _over 1.upto(10) _loop >> i _endloop
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new UnusedExpressionResultCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
