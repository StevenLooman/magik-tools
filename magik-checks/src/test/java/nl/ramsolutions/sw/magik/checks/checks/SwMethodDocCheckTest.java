package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test SwMethodDocCheck.
 */
class SwMethodDocCheckTest extends MagikCheckTestBase {

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.b(param1, param2?)\n"
            + "    ## This is an example method. PARAM1 and PARAM2? are used.\n"
            + "    ## Some more doc.\n"
            + "_endmethod",
        ""
            + "_method a.b\n"
            + "    ## This is an example method.\n"
            + "    ## Some more doc.\n"
            + "_endmethod",
        ""
            + "_method a.b(param1, param2, param3)\n"
            + "    ## There are PARAM1, PARAM2.\n"
            + "    ## And PARAM3\n"
            + "_endmethod",
    })
    void testValid(final String code) {
        final MagikCheck check = new SwMethodDocCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testDocMissing() {
        final MagikCheck check = new SwMethodDocCheck();
        final String code = ""
            + "_method a.b(param1, param2)\n"
            + "    ## This is an example method.\n"
            + "    ## Some more doc.\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.b\n"
            + "_endmethod",
        ""
            + "_method a.b\n"
            + "    ##\n"
            + "    ##\n"
            + "_endmethod",
    })
    void testInvalid(final String code) {
        final MagikCheck check = new SwMethodDocCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

}
