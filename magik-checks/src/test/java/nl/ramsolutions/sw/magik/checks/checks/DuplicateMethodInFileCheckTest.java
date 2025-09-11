package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link DuplicateMethodInFileCheck}. */
class DuplicateMethodInFileCheckTest {

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
    assertThat(check).reportsNoIssues(code);
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
    assertThat(check).reportsIssueCount(code, 2);
  }
}
