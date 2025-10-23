package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link SimplifyIfCheck}. */
class SimplifyIfCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _if a
        _then
        _else
            _err
        _endif
        """,
        """
        _if a
        _then
          _if b
          _then
          _endif
          c()
        _endif
        """,
        """
        _if a
        _then
          _if b
          _then
          _elif c
          _then
          _endif
        _endif
        """,
        """
        _if a
        _then
          _if b
          _then
          _else
          _endif
        _endif
        """,
        """
        _if a
        _then
        _else
          _if b
          _then
          _endif
          c()
        _endif
        """,
        """
        _if a
        _then
          _if b
          _then
          _endif
        _else
          c()
        _endif
    """,
      })
  void testValid(final String code) {
    final MagikCheck check = new SimplifyIfCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _if a
        _then
          _if b
          _then
          _endif
        _endif
        """,
        """
        _if a
        _then
          _if _err
          _then
          _endif
        _endif
        """,
        """
        _if a
        _then
        _else
          _if b
          _then
          _endif
        _endif
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new SimplifyIfCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
