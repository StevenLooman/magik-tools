package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.api.Test;

/** Test {@link MethodComplexityCheck}. */
@SuppressWarnings("checkstyle:MagicNumber")
class MethodComplexityCheckTest {

  @Test
  void testTooComplex() {
    final MethodComplexityCheck check = new MethodComplexityCheck();
    check.maxComplexity = 5;

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
    assertThat(check).reportsIssueCount(code, 1);
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
    assertThat(check).reportsNoIssues(code);
  }
}
