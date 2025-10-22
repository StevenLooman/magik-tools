package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link UseValueCompareCheck}. */
class UseValueCompareCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a _is b",
        "a = \"b\"",
        "a _is 1",
      })
  void testValid(final String code) {
    final MagikCheck check = new UseValueCompareCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\"a\" _is b",
        "a _is \"b\"",
        "a _is 0.0",
        "536870913 _is a",
        "a _is 16rffffffffffff",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new UseValueCompareCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
