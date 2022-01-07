package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test DuplicateMethodInFileCheck.
 */
class DuplicateMethodInFileCheckTest extends MagikCheckTestBase {

    @Test
    void testNoParameters() {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final String code = ""
            + "_method a.a\n"
            + "_endmethod\n"
            + "_method a.b\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testNoParametersDuplicate() {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final String code = ""
            + "_method a.a\n"
            + "_endmethod\n"
            + "_method a.a\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

    @Test
    void testParameters() {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final String code = ""
            + "_method a.a(p1)\n"
            + "_endmethod\n"
            + "_method a.a\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testParametersDuplicate() {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final String code = ""
            + "_method a.a(p1)\n"
            + "_endmethod\n"
            + "_method a.a(p1, p2)\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

    @Test
    void testParametersAssignment() {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final String code = ""
            + "_method a.a(p1)\n"
            + "_endmethod\n"
            + "_method a.a(p1, p2) << p3\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testParametersAssignmentDuplicate() {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final String code = ""
            + "_method a.a(p1) << p2\n"
            + "_endmethod\n"
            + "_method a.a(p1) << p2\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

    @Test
    void testIndexer() {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final String code = ""
            + "_method a[p1]\n"
            + "_endmethod\n"
            + "_method a[p1, p2]\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testIndexerDuplicate() {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final String code = ""
            + "_method a[p1]\n"
            + "_endmethod\n"
            + "_method a[p1]\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

    @Test
    void testIndexerAssignment() {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final String code = ""
            + "_method a[p1]\n"
            + "_endmethod\n"
            + "_method a[p1] << p2\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testIndexerAssignmentDuplicate() {
        final MagikCheck check = new DuplicateMethodInFileCheck();
        final String code = ""
            + "_method a[p1] << p2\n"
            + "_endmethod\n"
            + "_method a[p1] << p2\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

}
