package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link LhsRhsComparatorEqualCheck}. */
class LhsRhsComparatorCheckTest {

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
    assertThat(check).reportsNoIssues(code);
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
    assertThat(check).reportsIssueCount(code, 1);
  }
}
