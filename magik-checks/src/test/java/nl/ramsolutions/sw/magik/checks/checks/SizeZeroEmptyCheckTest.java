package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link SizeZeroEmptyCheck}. */
class SizeZeroEmptyCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a.size _is 0",
        "a.b.size _is 0",
        "0 _is a.size",
        "a.size = 0",
        "0 = a.size",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new SizeZeroEmptyCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testValid() {
    final MagikCheck check = new SizeZeroEmptyCheck();
    final String code = "a.empty?\n";
    assertThat(check).reportsNoIssues(code);
  }
}
