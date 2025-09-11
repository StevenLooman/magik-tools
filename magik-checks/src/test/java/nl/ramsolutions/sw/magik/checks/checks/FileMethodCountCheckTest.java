package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Test {@link FileMethodCountCheck}. */
class FileMethodCountCheckTest {

  @Test
  void testTooManyMethods() {
    final FileMethodCountCheck check = new FileMethodCountCheck();
    check.maxMethodCount = 2;
    final String code =
        """
        _method a.m1 _endmethod
        _method a.m1 _endmethod
        _method a.m1 _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Test
  void testOk() {
    final FileMethodCountCheck check = new FileMethodCountCheck();
    check.maxMethodCount = 10;
    final String code =
        """
        _method a.m1 _endmethod
        _method a.m1 _endmethod
        _method a.m1 _endmethod
        """;
    assertThat(check).reportsNoIssues(code);
  }
}
