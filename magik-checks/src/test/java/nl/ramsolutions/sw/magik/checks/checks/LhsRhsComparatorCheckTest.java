package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test LhsRhsComparatorEqualCheck. */
class LhsRhsComparatorCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a _or b",
        "a _xor b",
        "a _and b",
        "a _is b",
        "a >= b",
      })
  void testValid(final String code) {
    final MagikCheck check = new LhsRhsComparatorEqualCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a _or a",
        "a _xor a",
        "a _and a",
        "a _is a",
        "a >= a",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new LhsRhsComparatorEqualCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
