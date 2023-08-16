package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test DuplicateMethodInFileCheck.
 */
class DuplicateMethodInFileCheckTest extends MagikCheckTestBase {

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.a\n"
            + "_endmethod\n"
            + "_method a.b\n"
            + "_endmethod\n",
        ""
            + "_method a.a(p1)\n"
            + "_endmethod\n"
            + "_method a.a\n"
            + "_endmethod\n",
        ""
            + "_method a.a(p1)\n"
            + "_endmethod\n"
            + "_method a.a(p1, p2) << p3\n"
            + "_endmethod\n",
        ""
            + "_method a[p1]\n"
            + "_endmethod\n"
            + "_method a[p1, p2]\n"
            + "_endmethod\n",
        ""
            + "_method a[p1]\n"
            + "_endmethod\n"
            + "_method a[p1] << p2\n"
            + "_endmethod\n",
    })
    void testValid(final String code) {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.a\n"
            + "_endmethod\n"
            + "_method a.a\n"
            + "_endmethod\n",
        ""
            + "_method a.a(p1)\n"
            + "_endmethod\n"
            + "_method a.a(p1, p2)\n"
            + "_endmethod\n",
        ""
            + "_method a.a(p1) << p2\n"
            + "_endmethod\n"
            + "_method a.a(p1) << p2\n"
            + "_endmethod\n",
        ""
            + "_method a[p1]\n"
            + "_endmethod\n"
            + "_method a[p1]\n"
            + "_endmethod\n",
        ""
            + "_method a[p1] << p2\n"
            + "_endmethod\n"
            + "_method a[p1] << p2\n"
            + "_endmethod\n",
    })
    void testInvalid(final String code) {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

}
