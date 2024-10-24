package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test NestingDepthCheck. */
@SuppressWarnings("checkstyle:MagicNumber")
class NestingDepthCheckTest extends MagikCheckTestBase {

  @Test
  void testMethodExceedingMaximumNestingDepth() {
    final NestingDepthCheck check = new NestingDepthCheck();

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
  void testLoopAfterIfDoesCountAsExtraNestingDepth() {
    final NestingDepthCheck check = new NestingDepthCheck();

    final String code =
        """
        _method a.b
            _if a
            _then
                _loop
                    _if b
                    _then
                        _if c
                        _then
                        _endif
                    _endif
                _endloop
            _endif
        _endmethod
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testLoopAfterForDoesNotCountAsExtraNestingDepth() {
    final NestingDepthCheck check = new NestingDepthCheck();

    final String code =
        """
        _method a.b
            _if a
            _then
                _for i _over 1.upto(5)
                _loop
                    _if b
                    _then
                    _endif
                _endloop
            _endif
        _endmethod
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testNotExceedingMaximumNestingDepth() {
    final MagikCheck check = new NestingDepthCheck();
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

  @Test
  void testNotExceedingMaximumNestingDepthWithEarlyReturns() {
    final MagikCheck check = new NestingDepthCheck();
    final String code =
        """
        _method a.b
            _loop
                _if a _then _leave _endif
                _loop
                    _if b _then _leave _endif

                    _return c
                _endloop
            _endloop
        _endmethod
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
