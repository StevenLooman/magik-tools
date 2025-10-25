package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link UndefinedVariableCheck}. */
class UndefinedVariableCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _local l_a << 10
          write(l_a)
        _endmethod
        """,
        """
        _method a.b
          l_a << 10
          write(l_a)
        _endmethod
        """,
        """
        _proc()
          _local l_a
          write(l_a)
        _endproc
        """,
        """
        _method a.b(p_a)
        _endmethod
        """,
        """
        _method a.b()
          l_a << l_b << 10
          show(l_a, l_b)
        _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new UndefinedVariableCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          write(l_a)
        _endmethod
        """,
        """
        _proc()
          write(l_a)
        _endproc
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new UndefinedVariableCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
