package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link NoSelfUseCheck}. */
class NoSelfUseCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _self.m
        _endmethod
        """,
        """
        _method a.b
          _clone.m
        _endmethod
        """,
        """
        _method a.b
          _super.m
        _endmethod
        """,
        """
        _method a.b
          write(.slot)
        _endmethod
        """,
        """
        _abstract _method a.b
        _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new NoSelfUseCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testInvalid() {
    final MagikCheck check = new NoSelfUseCheck();
    final String code =
        """
        _method a.b
          show(1)
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }
}
