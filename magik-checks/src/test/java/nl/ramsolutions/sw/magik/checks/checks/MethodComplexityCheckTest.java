package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test MethodComplexityCheck. */
@SuppressWarnings("checkstyle:MagicNumber")
class MethodComplexityCheckTest extends MagikCheckTestBase {

  @Test
  void testTooComplex() {
    final MethodComplexityCheck check = new MethodComplexityCheck();
    check.maximumComplexity = 5;

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
    assertThat(issues).hasSize(1);
  }

  @Test
  void testNotTooComplex() {
    final MagikCheck check = new MethodComplexityCheck();
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
