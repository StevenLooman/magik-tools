package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link UnusedIfResultCheck}. */
class UnusedIfResultCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        // assigned with <<
        """
        x << _if a
             _then
                 >> 1
             _else
                 >> 2
             _endif
        """,
        // forwarded with >>
        """
        >> _if a
           _then
               >> 1
           _else
               >> 2
           _endif
        """,
        // returned with _return
        """
        _return _if a
                _then
                    >> 1
                _else
                    >> 2
                _endif
        """,
        // local variable assignment
        """
        _local x << _if a
                    _then
                        >> 1
                    _else
                        >> 2
                    _endif
        """,
        // used as method argument
        """
        foo(_if a
            _then
                >> 1
            _else
                >> 2
            _endif)
        """,
        // used as condition of outer _if
        """
        _if _if a
            _then
                >> _true
            _else
                >> _false
            _endif
        _then
            write("yes")
        _endif
        """,
        // used inside a _loop but assigned
        """
        _loop
            x << _if a _then >> 1 _else >> 2 _endif
        _endloop
        """,
        // chained assignment
        """
        x << y << _if a _then >> 1 _else >> 2 _endif
        """,
        // no >> in any branch — no value produced
        """
        _if a
        _then
            write("yes")
        _else
            write("no")
        _endif
        """,
        // >> only in nested inner _if which is assigned
        """
        _if outer
        _then
            x << _if inner _then >> 1 _else >> 2 _endif
        _endif
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new UnusedIfResultCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // standalone, both branches emit
        """
        _if _true
        _then
            >> _true
        _else
            >> _false
        _endif
        """,
        // standalone, only _then branch emits
        """
        _if a
        _then
            >> 1
        _endif
        """,
        // standalone, only _else branch emits
        """
        _if a
        _then
            write("a")
        _else
            >> 2
        _endif
        """,
        // standalone, emit in _elif
        """
        _if a
        _then
            write("a")
        _elif b
        _then
            >> 2
        _endif
        """,
        // standalone, multiple _elif all emit
        """
        _if a
        _then
            >> 1
        _elif b
        _then
            >> 2
        _elif c
        _then
            >> 3
        _endif
        """,
        // standalone inside a _loop body
        """
        _loop
            _if a _then >> 1 _else >> 2 _endif
        _endloop
        """,
        // standalone inside another _if's _then body
        """
        _if outer
        _then
            _if inner
            _then
                >> 1
            _else
                >> 2
            _endif
        _endif
        """,
        // standalone inside the _else of an outer _if
        """
        _if outer
        _then
            write("ok")
        _else
            _if inner _then >> 1 _else >> 2 _endif
        _endif
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new UnusedIfResultCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
