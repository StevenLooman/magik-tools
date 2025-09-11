package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.api.Test;

/** {@link UnsafeEvaluateInvocationCheck} tests. */
class UnsafeEvaluateInvocationCheckTest {

  @Test
  void testInvocationOk() {
    final MagikCheck check = new UnsafeEvaluateInvocationCheck();
    final String code = "'abc'.p";
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testInvocationUnsafeEvaluate() {
    final MagikCheck check = new UnsafeEvaluateInvocationCheck();
    final String code = "'abc'.unsafe_evaluate()";
    assertThat(check).reportsIssueCount(code, 1);
  }
}
