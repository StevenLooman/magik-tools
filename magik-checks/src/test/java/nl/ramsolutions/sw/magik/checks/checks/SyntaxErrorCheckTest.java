package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test SyntaxErrorCheck.
 */
class SyntaxErrorCheckTest extends MagikCheckTestBase {

    @Test
    void testSyntaxError() {
        final MagikCheck check = new SyntaxErrorCheck();
        final String code = ""
            + "_block\n"
            + "_endbloc";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testSytnaxError2() {
        final MagikCheck check = new SyntaxErrorCheck();
        final String code = "_method";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

}
