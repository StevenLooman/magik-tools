package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test ForbiddenGlobalUsageCheck. */
class ForbiddenGlobalUsageCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
        _endmethod
        """,
        """
        _method a.b
          _dynamic !current_grs! << x
        _endmethod
        """,
      })
  void testValid(final String code) {
    final ForbiddenGlobalUsageCheck check = new ForbiddenGlobalUsageCheck();
    check.forbiddenGlobals = "!current_world!";

    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _dynamic !current_world! << x
        _endmethod
        """,
        """
        _method a.b
          !current_world! << x
        _endmethod
        """,
        """
        _method a.b
          sw:!current_world! << x
        _endmethod
        """,
        """
        _method a.b
          (sw:!current_world!, !current_grs!) << (x, y)
        _endmethod
        """,
        """
        _method a.b
          x << !current_world!
        _endmethod
        """,
      })
  void testDynamicSet(final String code) {
    final ForbiddenGlobalUsageCheck check = new ForbiddenGlobalUsageCheck();
    check.forbiddenGlobals = "!current_world!";

    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMultiDynamicSet() {
    final ForbiddenGlobalUsageCheck check = new ForbiddenGlobalUsageCheck();
    check.forbiddenGlobals = "!current_grs!,!current_world!";

    final String code =
        """
        _method a.b
          _dynamic (!current_grs!, !current_world!) << (x, y)
        _endmethod
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(2);
  }
}
