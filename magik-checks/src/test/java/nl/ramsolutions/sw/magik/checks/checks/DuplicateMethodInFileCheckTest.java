package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test DuplicateMethodInFileCheck. */
class DuplicateMethodInFileCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.a
        _endmethod
        _method a.b
        _endmethod
        """,
        """
        _method a.a(p1)
        _endmethod
        _method a.a
        _endmethod
        """,
        """
        _method a.a(p1)
        _endmethod
        _method a.a(p1, p2) << p3
        _endmethod
        """,
        """
        _method a[p1]
        _endmethod
        _method a[p1, p2]
        _endmethod
        """,
        """
        _method a[p1]
        _endmethod
        _method a[p1] << p2
        _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new DuplicateMethodInFileCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.a
        _endmethod
        _method a.a
        _endmethod
        """,
        """
        _method a.a(p1)
        _endmethod
        _method a.a(p1, p2)
        _endmethod
        """,
        """
        _method a.a(p1) << p2
        _endmethod
        _method a.a(p1) << p2
        _endmethod
        """,
        """
        _method a[p1]
        _endmethod
        _method a[p1]
        _endmethod
        """,
        """
        _method a[p1] << p2
        _endmethod
        _method a[p1] << p2
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new DuplicateMethodInFileCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(2);
  }
}
