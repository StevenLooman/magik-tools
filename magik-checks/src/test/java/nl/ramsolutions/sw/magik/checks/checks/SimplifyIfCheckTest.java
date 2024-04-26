package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test SimplifyIfCheck. */
class SimplifyIfCheckTest extends MagikCheckTestBase {

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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
