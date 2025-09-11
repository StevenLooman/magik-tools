package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link HidesVariableCheck}. */
class HidesVariableCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _block
          _local a << 10
          _block
            _local b << 20
          _endblock
        _endblock
        """,
        """
        _block
          _local a << 10
          _proc()
            _import a
          _endproc
        _endblock
        """,
        """
        _block
          _block
            _local a << 20
          _endblock
          _local a << 10
        _endblock
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new HidesVariableCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _block
          _local a << 10
          _block
            _local a << 20
          _endblock
        _endblock
        """,
        """
        _block
          _local a << 10, b << 20
          _block
            _local a << 20
          _endblock
        _endblock
        """,
        """
        _block
          _local a << 10
          _block
            _local b << 20, a << 30
          _endblock
        _endblock
        """,
        """
        _block
          _local (a, b) << (10, 20)
          _block
            _local a << 20
          _endblock
        _endblock
        """,
        """
        _block
          _local a << 10
          _block
            _local (b, a) << (20, 30)
          _endblock
        _endblock
        """,
        """
        _method a.b
          a << 10
          _local a << 20
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new HidesVariableCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
