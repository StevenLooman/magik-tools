package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test TrailingWhitespaceCheck.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class TrailingWhitespaceCheckTest extends MagikCheckTestBase {

    @Test
    void testNoTrailingWhitespace() {
        final MagikCheck check = new TrailingWhitespaceCheck();
        final String code = "a";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testTrailingWhitespace() {
        final MagikCheck check = new TrailingWhitespaceCheck();
        final String code = "a    ";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testTrailingWhitespaceMultiline() {
        final MagikCheck check = new TrailingWhitespaceCheck();
        final String code = ""
            + "a()    \n"
            + "         \n"
            + "b()    \n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(3);
    }

}
