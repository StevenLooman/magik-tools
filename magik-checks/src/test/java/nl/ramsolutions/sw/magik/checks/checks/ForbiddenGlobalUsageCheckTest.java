package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ForbiddenGlobalUsageCheck.
 */
class ForbiddenGlobalUsageCheckTest extends MagikCheckTestBase {

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.b\n"
            + "_endmethod\n",
        ""
            + "_method a.b\n"
            + "  _dynamic !current_grs! << x\n"
            + "_endmethod\n",
    })
    void testValid(final String code) {
        final ForbiddenGlobalUsageCheck check = new ForbiddenGlobalUsageCheck();
        check.forbiddenGlobals = "!current_world!";

        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.b\n"
            + "  _dynamic !current_world! << x\n"
            + "_endmethod\n",
        ""
            + "_method a.b\n"
            + "  !current_world! << x\n"
            + "_endmethod\n",
        ""
            + "_method a.b\n"
            + "  sw:!current_world! << x\n"
            + "_endmethod\n",
        ""
            + "_method a.b\n"
            + "  (sw:!current_world!, !current_grs!) << (x, y)\n"
            + "_endmethod\n",
        ""
            + "_method a.b\n"
            + "  x << !current_world!\n"
            + "_endmethod\n",
    })
    void testDynamicSet(final String code) {
        final ForbiddenGlobalUsageCheck check = new ForbiddenGlobalUsageCheck();
        check.forbiddenGlobals = "!current_world!";

        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMultiDynamicSet() {
        final ForbiddenGlobalUsageCheck check = new ForbiddenGlobalUsageCheck();
        check.forbiddenGlobals = "!current_grs!,!current_world!";

        final String code = ""
            + "_method a.b\n"
            + "  _dynamic (!current_grs!, !current_world!) << (x, y)\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

}
