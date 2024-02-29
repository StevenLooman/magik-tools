package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test VariableNamingCheck. */
class VariableNamingCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "_local coord",
        "coord << 10",
        "_local x",
        "_method a.b(coord) _endmethod",
        "_local l_coord",
        "_local l_x",
        "_local (l_item, l_result) << (1, 2)",
        "result +<< 10",
        "result +<< str << _self.a",
      })
  void testValid(final String code) {
    final MagikCheck check = new VariableNamingCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "_local c",
        "c << 10",
        "_method a.b(c) _endmethod",
        "_local l_c",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new VariableNamingCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMultiVariableDeclarationInvalidName() {
    final MagikCheck check = new VariableNamingCheck();
    final String code = "_local (l_i, l_r) << (1, 2)";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(2);
  }
}
