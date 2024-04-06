package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test EmptyBlockCheck. */
class EmptyBlockCheckTest extends MagikCheckTestBase {

  @Test
  void testBlock() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code = """
        _block
            write(a)
        _endblock""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testBlockEmpty() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code = """
        _block
        _endblock""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testIfBlock() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code = """
        _if a
        _then
            write(a)
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testIfBlockEmpty() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code = """
        _if a
        _then
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testIfElifBlock() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code =
        """
        _if a
        _then
            write(a)
        _elif b
        _then
            write(b)
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testIfElifBlockEmpty() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code =
        """
        _if a
        _then
            write(a)
        _elif b
        _then
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testIfElseBlock() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code =
        """
        _if a
        _then
            write(a)
        _else
            write(b)
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testIfElseBlockEmpty() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code =
        """
        _if a
        _then
            write(a)
        _else
        _endif""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testProtectEmpty() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code =
        """
        _protect
        _protection
            write(a)
        _endprotect""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testProtectionEmpty() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code =
        """
        _protect
            write(a)
        _protection
        _endprotect""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMethodEmpty() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code = """
        _method a.b
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testAbstractMethodEmpty() {
    final MagikCheck check = new EmptyBlockCheck();

    final String code = """
        _abstract _method a.b
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
