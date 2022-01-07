package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test MethodComplexityCheck.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class MethodComplexityCheckTest extends MagikCheckTestBase {

    @Test
    void testTooComplex() {
        final MethodComplexityCheck check = new MethodComplexityCheck();
        check.maximumComplexity = 5;

        final String code = ""
            + "_method a.b\n"
            + "    _if a\n"
            + "    _then\n"
            + "        _if b\n"
            + "        _then\n"
            + "            _if c\n"
            + "            _then\n"
            + "                _if d\n"
            + "                _then\n"
            + "                    _if e\n"
            + "                    _then\n"
            + "                    _endif\n"
            + "                _endif\n"
            + "            _endif\n"
            + "        _endif\n"
            + "    _endif\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testNotTooComplex() {
        final MagikCheck check = new MethodComplexityCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _if a"
            + "    _then"
            + "    _endif\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
