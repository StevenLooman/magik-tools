package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test SimplifyIfCheck. */
class SimplifyIfCheckTest extends MagikCheckTestBase {

  @Test
  void testSimplifyIfIf() {
    final MagikCheck check = new SimplifyIfCheck();
    final String code =
        """
        _if a
        _then
            _if b
            _then
            _endif
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testSimplifyIfIfSyntaxError() {
    final MagikCheck check = new SimplifyIfCheck();
    final String code =
        """
        _if a
        _then
            _if _err
            _then
            _endif
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testSimplifyIfElseIf() {
    final MagikCheck check = new SimplifyIfCheck();
    final String code =
        """
        _if a
        _then
        _else
            _if b
            _then
            _endif
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testSimplifyIfElseIfSyntaxError() {
    final MagikCheck check = new SimplifyIfCheck();
    final String code =
        """
        _if a
        _then
        _else
            _err
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testNoSimplifyIfIf() {
    final MagikCheck check = new SimplifyIfCheck();
    final String code =
        """
        _if a
        _then
            _if b
            _then
            _endif
            c()
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testNoSimplifyIfIfElif() {
    final MagikCheck check = new SimplifyIfCheck();
    final String code =
        """
        _if a
        _then
            _if b
            _then
            _elif c
            _then
            _endif
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testNoSimplifyIfIfElse() {
    final MagikCheck check = new SimplifyIfCheck();
    final String code =
        """
        _if a
        _then
            _if b
            _then
            _else
            _endif
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testNoSimplifyIfElseIf() {
    final MagikCheck check = new SimplifyIfCheck();
    final String code =
        """
        _if a
        _then
        _else
            _if b
            _then
            _endif
            c()
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testSimplifyIfIfElseIf() {
    final MagikCheck check = new SimplifyIfCheck();
    final String code =
        """
        _if a
        _then
            _if b
            _then
            _endif
        _else
            c()
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
