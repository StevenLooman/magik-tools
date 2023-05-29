package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test MagikCheckTestBase.
 */
class NoSelfUseCheckTest extends MagikCheckTestBase {

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.b\n"
            + "    _self.m\n"
            + "_endmethod",
        ""
            + "_method a.b\n"
            + "    _clone.m\n"
            + "_endmethod",
        ""
            + "_method a.b\n"
            + "    _super.m\n"
            + "_endmethod",
        ""
            + "_method a.b\n"
            + "    write(.slot)\n"
            + "_endmethod",
        ""
            + "_abstract _method a.b\n"
            + "_endmethod"
    })
    void testValid(final String code) {
        final MagikCheck check = new NoSelfUseCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testInvalid() {
        final MagikCheck check = new NoSelfUseCheck();
        final String code = ""
            + "_method a.b\n"
            + "    show(1)\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

}
