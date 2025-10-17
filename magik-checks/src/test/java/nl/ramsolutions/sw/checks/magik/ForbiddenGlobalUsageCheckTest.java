package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link ForbiddenGlobalUsageCheck}. */
class ForbiddenGlobalUsageCheckTest {

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

    assertThat(check).reportsNoIssues(code);
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

    assertThat(check).reportsIssueCount(code, 1);
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
    assertThat(check).reportsIssueCount(code, 2);
  }
}
