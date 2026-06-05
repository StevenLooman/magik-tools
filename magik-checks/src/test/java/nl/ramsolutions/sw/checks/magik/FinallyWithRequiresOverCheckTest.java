package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link FinallyWithRequiresOverCheck}. */
class FinallyWithRequiresOverCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _over a()
          _loop
          _finally _with x
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _for i _over a()
          _loop
          _finally _with x
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _loop
          _finally
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _while b()
          _loop
          _finally
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _loop
          _endloop
        _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new FinallyWithRequiresOverCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _loop
          _finally _with x
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _while b()
          _loop
          _finally _with x
          _endloop
        _endmethod
        """,
        """
        _method a.b
          _loop
          _finally _with _gather x
          _endloop
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new FinallyWithRequiresOverCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
