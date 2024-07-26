package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test FileMethodCountCheck. */
class FileMethodCountCheckTest extends MagikCheckTestBase {

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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
