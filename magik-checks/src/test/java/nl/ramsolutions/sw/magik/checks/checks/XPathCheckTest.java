package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test XPathCheck.
 */
class XPathCheckTest extends MagikCheckTestBase {

    @Test
    void testMatch() {
        final XPathCheck check = new XPathCheck();
        check.xpathQuery = "//RETURN_STATEMENT";

        final String code = ""
            + "_method a.b\n"
            + "    _return _self\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testNoMatch() {
        final XPathCheck check = new XPathCheck();
        check.xpathQuery = "//IF";

        final String code = ""
            + "_method a.b\n"
            + "    _return _self\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
