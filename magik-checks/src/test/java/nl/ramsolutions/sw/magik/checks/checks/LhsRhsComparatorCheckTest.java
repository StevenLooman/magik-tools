package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test LhsRhsComparatorEqualCheck.
 */
class LhsRhsComparatorCheckTest extends MagikCheckTestBase {

    @Test
    void testOrLhsRhsUnequal() {
        final MagikCheck check = new LhsRhsComparatorEqualCheck();
        final String code = "a _or b\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testOrLhsRhsEqual() {
        final MagikCheck check = new LhsRhsComparatorEqualCheck();
        final String code = "a _or a\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testXorLhsRhsUnequal() {
        final MagikCheck check = new LhsRhsComparatorEqualCheck();
        final String code = "a _xor b\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testXorLhsRhsEqual() {
        final MagikCheck check = new LhsRhsComparatorEqualCheck();
        final String code = "a _xor a\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testAndLhsRhsUnequal() {
        final MagikCheck check = new LhsRhsComparatorEqualCheck();
        final String code = "a _and b\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testAndLhsRhsEqual() {
        final MagikCheck check = new LhsRhsComparatorEqualCheck();
        final String code = "a _and a\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testIsLhsRhsUnequal() {
        final MagikCheck check = new LhsRhsComparatorEqualCheck();
        final String code = "a _is b\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testIsLhsRhsEqual() {
        final MagikCheck check = new LhsRhsComparatorEqualCheck();
        final String code = "a _is a\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testGeLhsRhsUnequal() {
        final MagikCheck check = new LhsRhsComparatorEqualCheck();
        final String code = "a >= b\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testGeLhsRhsEqual() {
        final MagikCheck check = new LhsRhsComparatorEqualCheck();
        final String code = "a >= a\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

}
