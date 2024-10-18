package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test NestingLevelCheck. */
@SuppressWarnings("checkstyle:MagicNumber")
class NestingLevelCheckTest extends MagikCheckTestBase {

  @Test
  void testTooComplex() {
    final NestingLevelCheck check = new NestingLevelCheck();

    final String code =
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
                            _endif
                        _endif
                    _endif
                _endif
            _endif
        _endmethod
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(2); // FIXME: Should be 1 since we didn't close the first `_if`-statement yet.
  }

  @Test
  void testNotTooComplex() {
    final MagikCheck check = new NestingLevelCheck();
    final String code =
        """
        _method a.b
            _if a
            _then
            _endif
        _endmethod
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
