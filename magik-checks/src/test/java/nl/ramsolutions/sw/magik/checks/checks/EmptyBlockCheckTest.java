package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test EmptyBlockCheck. */
class EmptyBlockCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _block
            write(a)
        _endblock
        """,
        """
        _if a
        _then
            write(a)
        _endif
        """,
        """
        _if a
        _then
            write(a)
        _elif b
        _then
            write(b)
        _endif
        """,
        """
          _if a
          _then
              write(a)
          _else
              write(b)
          _endif
          """,
        """
        _abstract _method a.b
        _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new EmptyBlockCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _block
        _endblock
        """,
        """
        _if a
        _then
        _endif
        """,
        """
        _if a
        _then
            write(a)
        _elif b
        _then
        _endif
        """,
        """
        _if a
        _then
            write(a)
        _else
        _endif
        """,
        """
        _protect
        _protection
            write(a)
        _endprotect
        """,
        """
        _protect
            write(a)
        _protection
        _endprotect
        """,
        """
        _method a.b
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new EmptyBlockCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
