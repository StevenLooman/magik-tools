package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link EmptyBlockCheck}. */
class EmptyBlockCheckTest {

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
    assertThat(check).reportsNoIssues(code);
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
    assertThat(check).reportsIssueCount(code, 1);
  }
}
