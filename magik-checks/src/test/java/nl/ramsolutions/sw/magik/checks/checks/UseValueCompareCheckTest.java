package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test UseValueCompareCheck.
 */
class UseValueCompareCheckTest extends MagikCheckTestBase {

    @Test
    void testNoLiteral() {
        final MagikCheck check = new UseValueCompareCheck();
        final String code = "a _is b";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testStringValueCompare() {
        final MagikCheck check = new UseValueCompareCheck();
        final String code = "a = \"b\"";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testStringInstanceCompareLeft() {
        final MagikCheck check = new UseValueCompareCheck();
        final String code = "\"a\" _is b";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testStringInstanceCompareRight() {
        final MagikCheck check = new UseValueCompareCheck();
        final String code = "a _is \"b\"";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testIntegerInstanceCompare() {
        final MagikCheck check = new UseValueCompareCheck();
        final String code = "a _is 1";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testBignumInstanceCompare() {
        final MagikCheck check = new UseValueCompareCheck();
        final String code = "\"a\" _is 536870912";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testBignumFloat() {
        final MagikCheck check = new UseValueCompareCheck();
        final String code = "\"a\" _is 0.0";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

}
