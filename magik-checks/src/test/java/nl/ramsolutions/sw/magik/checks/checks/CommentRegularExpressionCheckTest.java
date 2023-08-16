package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test CommentRegularExpressionCheck.
 */
class CommentRegularExpressionCheckTest extends MagikCheckTestBase {

    @Test
    void testMatch() {
        final CommentRegularExpressionCheck check = new CommentRegularExpressionCheck();
        check.regularExpression = ".*match_me.*";

        final String code = ""
            + "# match_me\n"
            + "print(a)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testNoMatch() {
        final CommentRegularExpressionCheck check = new CommentRegularExpressionCheck();
        check.regularExpression = ".*do_not_match_me.*";

        final String code = ""
            + "# match_me\n"
            + "print(a)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }
}
