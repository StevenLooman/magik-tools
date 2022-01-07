package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test EmptyBlockCheck.
 */
class EmptyBlockCheckTest extends MagikCheckTestBase {

    @Test
    void testBlock() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_block\n"
            + "    write(a)\n"
            + "_endblock";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testBlockEmpty() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_block\n"
            + "_endblock";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testIfBlock() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_if a\n"
            + "_then\n"
            + "    write(a)\n"
            + "_endif";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testIfBlockEmpty() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_if a\n"
            + "_then\n"
            + "_endif";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testIfElifBlock() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_if a\n"
            + "_then\n"
            + "    write(a)\n"
            + "_elif b\n"
            + "_then\n"
            + "    write(b)\n"
            + "_endif";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testIfElifBlockEmpty() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_if a\n"
            + "_then\n"
            + "    write(a)\n"
            + "_elif b\n"
            + "_then\n"
            + "_endif";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testIfElseBlock() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_if a\n"
            + "_then\n"
            + "    write(a)\n"
            + "_else\n"
            + "    write(b)\n"
            + "_endif";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testIfElseBlockEmpty() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_if a\n"
            + "_then\n"
            + "    write(a)\n"
            + "_else\n"
            + "_endif";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testProtectEmpty() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_protect\n"
            + "_protection\n"
            + "    write(a)\n"
            + "_endprotect";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testProtectionEmpty() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_protect\n"
            + "    write(a)\n"
            + "_protection\n"
            + "_endprotect";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMethodEmpty() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_method a.b\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testAbstractMethodEmpty() {
        final MagikCheck check = new EmptyBlockCheck();

        final String code = ""
            + "_abstract _method a.b\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
