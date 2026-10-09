package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link InvalidAssignmentTargetCheck}. */
class InvalidAssignmentTargetCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "a << 3",
        "a << b << 3",
        "a ^<< 3",
        ".slot << 3",
        "!dyn! << 3",
        "sw:glob << 3",
        "_local a << 3",
        "(a) << 3",
        "(a, b) << (1, 2)",
        "(a, _gather b) << p()",
        "(.slot, a.x, a[1]) << p()",
        "a.x << 3",
        "a.x() << 3",
        "a.x.p() << 3",
        "a[1] << 3",
        "a << b.x << 3",
        "a +<< 3",
        ".slot +<< 3",
        "a.x +<< 3",
        "a[1] +<< 3",
        "a _orif<< b",
        "a << p()",
        "a << p() + 3",
      })
  void testValid(final String code) {
    final MagikCheck check = new InvalidAssignmentTargetCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "p() << 3",
        "a << p() << 3",
        "1 << 2",
        "\"a\" << 2",
        ":sym << 2",
        "{1} << 2",
        "_self << 2",
        "_unset << 2",
        "_true << 2",
        "_thisthread << 2",
        "@sw:glob << 2",
        "a + b << 2",
        "(a + 1) << 2",
        "(a, p()) << (1, 2)",
        "(a, _gather p()) << q()",
        "p() +<< 2",
        "1 +<< 2",
        "a << 1 +<< 2",
      })
  void testInvalid(final String code) {
    final MagikCheck check = new InvalidAssignmentTargetCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
