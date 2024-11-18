package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test NestingDepthCheck. */
@SuppressWarnings("checkstyle:MagicNumber")
class NestingDepthCheckTest extends MagikCheckTestBase {

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

    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
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

    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
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

    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
