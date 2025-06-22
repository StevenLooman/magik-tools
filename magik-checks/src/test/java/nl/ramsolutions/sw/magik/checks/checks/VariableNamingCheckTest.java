package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link VariableNamingCheck}. */
class VariableNamingCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _block
          _local coord
        _endblock
        """,
        """
        coord << 10
        """,
        """
        _block
          _local x
        _endblock
        """,
        """
        _block
          _local id
        _endblock
        """,
        """
        _method a.b(coord)
        _endmethod
        """,
        """
        _block
          _local l_coord
        _endblock
        """,
        """
        _block
          _local l_x
        _endblock
        """,
        """
        _block
          _local l_id
        _endblock
        """,
        """
        _block
          _local (l_item, l_result) << (1, 2)
        _endblock
        """,
        """
        result +<< 10
        """,
        """
        result +<< str << _self.a
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new VariableNamingCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _local c
        _endmethod
        """,
        """
        _method a.b
          _local l_c
        _endmethod
        """,
        """
        _method a.b(c)
        _endmethod
        """,
        """
        _method a.b
          c << 10
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new VariableNamingCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
          _local this_is_the_very_long_variable_name
        _endmethod
        """,
        """
        _method a.b
          _local l_this_is_the_very_long_variable_name
        _endmethod
        """,
        """
        _method a.b
          this_is_the_very_long_variable_name << 10
        _endmethod
        """,
        """
        _method a.b(this_is_the_very_long_variable_name)
        _endmethod
        """,
      })
  void testTooLongVariableName(final String code) {
    final MagikCheck check = new VariableNamingCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMultiVariableDeclarationInvalidName() {
    final MagikCheck check = new VariableNamingCheck();
    final String code =
        """
    _block
      _local (l_i, l_r) << (1, 2)
    _endblock
    """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(2);
  }
}
