package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ForbiddenCallCheck.
 */
class ForbiddenCallCheckTest extends MagikCheckTestBase {

    @Test
    void testOk() {
        final MagikCheck check = new ForbiddenCallCheck();
        final String code = "do_something(1)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testProcedureShow() {
        final MagikCheck check = new ForbiddenCallCheck();
        final String code = "show(1)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMethodP() {
        final ForbiddenCallCheck check = new ForbiddenCallCheck();
        check.forbiddenCalls = ".p";
        final String code = "1.p";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

}
