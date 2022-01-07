package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test SizeZeroEmptyCheck.
 */
class SizeZeroEmptyCheckTest extends MagikCheckTestBase {

    @Test
    void testSizeIsZero() {
        final MagikCheck check = new SizeZeroEmptyCheck();
        final String code = "a.size _is 0";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testSizeIsZeroTraversed() {
        final MagikCheck check = new SizeZeroEmptyCheck();
        final String code = "a.b.size _is 0";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testSizeIsZeroReversed() {
        final MagikCheck check = new SizeZeroEmptyCheck();
        final String code = "0 _is a.size";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testSizeEqualsZero() {
        final MagikCheck check = new SizeZeroEmptyCheck();
        final String code = "a.size = 0";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testSizeEqualsZeroReversed() {
        final MagikCheck check = new SizeZeroEmptyCheck();
        final String code = "0 = a.size";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testEmpty() {
        final MagikCheck check = new SizeZeroEmptyCheck();
        final String code = "a.empty?\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
