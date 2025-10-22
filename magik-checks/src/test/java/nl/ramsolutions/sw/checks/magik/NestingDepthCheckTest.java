package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link NestingDepthCheck}. */
@SuppressWarnings("checkstyle:MagicNumber")
class NestingDepthCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _block
          _if a
          _then
            _loop
              _if b
              _then
                _if c
                _then
                  write("Too deep!")
                _endif
              _endif
            _endloop
          _endif
        _endblock
        """,
        """
        _proc()
          _if a
          _then
            _loop
              _if b
              _then
                _if c
                _then
                  write("Too deep!")
                _endif
              _endif
            _endloop
          _endif
        _endproc
        """,
        """
        _method a.b
          _if a
          _then
            _if b
            _then
              _if c
              _then
                _if d
                _then
                  _if e
                  _then
                    write("Too deep!")
                  _endif
                _endif
              _endif
            _endif
          _endif
        _endmethod
        """,
        """
        _method a.b
          _if a
          _then
            _loop
              _if b
              _then
                _if c
                _then
                  write("Too deep!")
                _endif
              _endif
            _endloop
          _endif
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new NestingDepthCheck();

    assertThat(check).reportsIssueCount(code, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _if a
          _then
            _for i _over 1.upto(5)
            _loop
              write("Okay!")
            _endloop
          _endif
        _endmethod
        """,
        """
        _method a.b
          _if a
          _then
            write("Okay!")
          _endif
        _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new NestingDepthCheck();

    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _loop
              _if a _then _return b _endif

              _loop
                _if c _then _return d _endif
              _endloop
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _loop
              _return b

              _loop
                _if c _then _return d _endif
              _endloop
          _endloop
        _endmethod
        """,
      })
  void testValidWithEarlyReturnsNotAddingToNestingDepth(final String code) {
    final NestingDepthCheck check = new NestingDepthCheck();
    check.countEarlyReturnAsNestingDepth = false;

    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testHandlingIsNotCountedAsValidEarlyReturnWithCountEarlyReturnEnabled() {
    final NestingDepthCheck check = new NestingDepthCheck();
    check.countEarlyReturnAsNestingDepth = true;

    final String code =
        """
        _method a.b
          _handling some_error _with _proc@mute() _return _endproc

          _if a
          _then
            _loop
              _if c
              _then
                write("Too deep!")
              _endif
            _endloop
          _endif
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testHandlingIsNotCountedAsValidEarlyReturnWithCountEarlyReturnDisabled() {
    final NestingDepthCheck check = new NestingDepthCheck();
    check.countEarlyReturnAsNestingDepth = false;

    final String code =
        """
        _method a.b
          _handling some_error _with _proc@mute() _return _endproc

          _if a
          _then
            _loop
              _if c
              _then
                write("Too deep!")
              _endif
            _endloop
          _endif
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }
}
