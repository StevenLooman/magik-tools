package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test HidesVariableCheck.
 */
class HidesVariableCheckTest extends MagikCheckTestBase {

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_block\n"
            + "    _local a << 10\n"
            + "    _block\n"
            + "        _local b << 20\n"
            + "    _endblock\n"
            + "_endblock\n",
        ""
            + "_block\n"
            + "    _local a << 10\n"
            + "    _proc()\n"
            + "        _import a\n"
            + "    _endproc\n"
            + "_endblock\n",
    })
    void testValid(final String code) {
        final MagikCheck check = new HidesVariableCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_block\n"
            + "    _local a << 10\n"
            + "    _block\n"
            + "        _local a << 20\n"
            + "    _endblock\n"
            + "_endblock\n",
        ""
            + "_block\n"
            + "    _local a << 10, b << 20\n"
            + "    _block\n"
            + "        _local a << 20\n"
            + "    _endblock\n"
            + "_endblock\n",
        ""
            + "_block\n"
            + "    _local a << 10\n"
            + "    _block\n"
            + "        _local b << 20, a << 30\n"
            + "    _endblock\n"
            + "_endblock\n",
        ""
            + "_block\n"
            + "    _local (a, b) << (10, 20)\n"
            + "    _block\n"
            + "        _local a << 20\n"
            + "    _endblock\n"
            + "_endblock\n",
        ""
            + "_block\n"
            + "    _local a << 10\n"
            + "    _block\n"
            + "        _local (b, a) << (20, 30)\n"
            + "    _endblock\n"
            + "_endblock\n",
    })
    void testInvalid(final String code) {
        final MagikCheck check = new HidesVariableCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

}
