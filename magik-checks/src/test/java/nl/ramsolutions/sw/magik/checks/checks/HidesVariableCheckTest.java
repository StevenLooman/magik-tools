package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test HidesVariableCheck. */
class HidesVariableCheckTest extends MagikCheckTestBase {

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
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
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
      })
  void testInvalid(final String code) {
    final MagikCheck check = new HidesVariableCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
