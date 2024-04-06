package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test SwMethodDocCheck. */
@SuppressWarnings("checkstyle:MagicNumber")
class SwMethodDocCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b(param1, param2?)
            ## This is an example method. PARAM1 and PARAM2? are used.
            ## Some more doc.
        _endmethod""",
        """
        _method a.b
            ## This is an example method.
            ## Some more doc.
        _endmethod""",
        """
        _method a.b(param1, param2, param3)
            ## There are PARAM1, PARAM2.
            ## And PARAM3
        _endmethod""",
      })
  void testValid(final String code) {
    final MagikCheck check = new SwMethodDocCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testDocMissing() {
    final MagikCheck check = new SwMethodDocCheck();
    final String code =
        """
        _method a.b(param1, param2)
            ## This is an example method.
            ## Some more doc.
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
        _endmethod""",
        """
        _method a.b
            ##
            ##
        _endmethod""",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new SwMethodDocCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
        _endmethod""",
        """
        _method a.b
            a.do_something()
        _endmethod""",
        """
        _method a.b()
        _endmethod""",
        """
        _method a.b()
            a.do_something()
        _endmethod""",
      })
  void testNotAllowBlankMethodDoc(final String code) {
    final SwMethodDocCheck check = new SwMethodDocCheck();
    check.allowBlankMethodDoc = false; // Defaults to false, but to be explicit.
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
        _endmethod""",
        """
        _method a.b
            a.do_something()
        _endmethod""",
        """
        _method a.b()
        _endmethod""",
        """
        _method a.b()
            a.do_something()
        _endmethod""",
      })
  void testAllowBlankMethodDoc(final String code) {
    final SwMethodDocCheck check = new SwMethodDocCheck();
    check.allowBlankMethodDoc = true;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
