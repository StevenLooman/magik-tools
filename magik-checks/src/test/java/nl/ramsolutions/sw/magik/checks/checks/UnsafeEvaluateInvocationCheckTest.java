package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UnsafeEvaluateInvocationCheck} tests.
 */
class UnsafeEvaluateInvocationCheckTest extends MagikCheckTestBase {

    @Test
    void testInvocationOk() {
        final MagikCheck check = new UnsafeEvaluateInvocationCheck();
        final String code = "'abc'.p";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testInvocationUnsafeEvaluate() {
        final MagikCheck check = new UnsafeEvaluateInvocationCheck();
        final String code = "'abc'.unsafe_evaluate()";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

}
