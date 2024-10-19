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
  void testExceedingMaximumNestingLevel() {
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
    assertThat(issues).hasSize(1);
  }

  @Test
  void testLoopAfterIfDoesCountAsExtraNestingLevel() {
    final NestingLevelCheck check = new NestingLevelCheck();

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
  void testLoopAfterForDoesNotCountAsExtraNestingLevel() {
    final NestingLevelCheck check = new NestingLevelCheck();

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
  void testNotExceedingMaximumNestingLevel() {
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
