package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test MagikCheckTestBase. */
class NoSelfUseCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b
            _self.m
        _endmethod""",
        """
        _method a.b
            _clone.m
        _endmethod""",
        """
        _method a.b
            _super.m
        _endmethod""",
        """
        _method a.b
            write(.slot)
        _endmethod""",
        """
        _abstract _method a.b
        _endmethod""",
      })
  void testValid(final String code) {
    final MagikCheck check = new NoSelfUseCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testInvalid() {
    final MagikCheck check = new NoSelfUseCheck();
    final String code =
        """
        _method a.b
            show(1)
        _endmethod""";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
