package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link SwMethodDocCheck}. */
@SuppressWarnings("checkstyle:MagicNumber")
class SwMethodDocCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b(param1, param2?)
          ## This is an example method. PARAM1 and PARAM2? are used.
          ## Some more doc.
        _endmethod
        """,
        """
        _method a.b
          ## This is an example method.
          ## Some more doc.
        _endmethod
        """,
        """
        _method a.b(param1, param2, param3)
          ## There are PARAM1, PARAM2.
          ## And PARAM3
        _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new SwMethodDocCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testDocMissing() {
    final MagikCheck check = new SwMethodDocCheck();
    final String code =
        """
        _method a.b(param1, param2)
          ## This is an example method.
          ## Some more doc.
        _endmethod
        """;
    assertThat(check).reportsIssueCount(code, 2);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
        _endmethod
        """,
        """
        _method a.b
          ##
          ##
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new SwMethodDocCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
        _endmethod
        """,
        """
        _method a.b
          a.do_something()
        _endmethod
        """,
        """
        _method a.b()
        _endmethod
        """,
        """
        _method a.b()
          a.do_something()
        _endmethod
        """,
      })
  void testNotAllowBlankMethodDoc(final String code) {
    final SwMethodDocCheck check = new SwMethodDocCheck();
    check.allowBlankMethodDoc = false; // Defaults to false, but to be explicit.
    assertThat(check).reportsIssueCount(code, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
        _endmethod
        """,
        """
        _method a.b
          a.do_something()
        _endmethod
        """,
        """
        _method a.b()
        _endmethod
        """,
        """
        _method a.b()
          a.do_something()
        _endmethod
        """,
      })
  void testAllowBlankMethodDoc(final String code) {
    final SwMethodDocCheck check = new SwMethodDocCheck();
    check.allowBlankMethodDoc = true;
    assertThat(check).reportsNoIssues(code);
  }
}
