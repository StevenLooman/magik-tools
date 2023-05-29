package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test SizeZeroEmptyCheck.
 */
class SizeZeroEmptyCheckTest extends MagikCheckTestBase {

    @ParameterizedTest
    @ValueSource(strings = {
        "a.size _is 0",
        "a.b.size _is 0",
        "0 _is a.size",
        "a.size = 0",
        "0 = a.size",
    })
    void testInvalid(final String code) {
        final MagikCheck check = new SizeZeroEmptyCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testValid() {
        final MagikCheck check = new SizeZeroEmptyCheck();
        final String code = "a.empty?\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
