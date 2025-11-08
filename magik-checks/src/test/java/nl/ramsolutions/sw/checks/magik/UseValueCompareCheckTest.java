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
        "a _isnt b",
        "a = \"b\"",
        "a <> \"b\"",
        "a _is 1",
        "a _isnt 1",
        "536870913 = a",
        "536870913 <> a",
      })
  void testValid(final String code) {
    final MagikCheck check = new UseValueCompareCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\"a\" _is b",
        "\"a\" _isnt b",
        "a _is \"b\"",
        "a _isnt \"b\"",
        "a _is 0.0",
        "a _isnt 0.0",
        "536870913 _is a",
        "536870913 _isnt a",
        "a _is 16rffffffffffff",
        "a _isnt 16rffffffffffff",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new UseValueCompareCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
