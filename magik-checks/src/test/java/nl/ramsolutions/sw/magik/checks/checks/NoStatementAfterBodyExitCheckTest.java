package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test NoStatementAfterBodyExitCheck.
 */
class NoStatementAfterBodyExitCheckTest extends MagikCheckTestBase {

    @Test
    void testNoStatementAfterReturn() {
        final MagikCheck check = new NoStatementAfterBodyExitCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _return 10\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testStatementAfterReturn() {
        final MagikCheck check = new NoStatementAfterBodyExitCheck();
        final String code = ""
            + "_method a.b\n"
            + "    >> 10\n"
            + "    write(10)\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testNoStatementAfterReturnComment() {
        final MagikCheck check = new NoStatementAfterBodyExitCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _return 10\n"
            + "    \n"
            + "    # comment\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testNoStatementAfterEmit() {
        final MagikCheck check = new NoStatementAfterBodyExitCheck();
        final String code = ""
            + "_method a.b\n"
            + "    >> 10\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testStatementAfterEmit() {
        final MagikCheck check = new NoStatementAfterBodyExitCheck();
        final String code = ""
            + "_method a.b\n"
            + "    >> 10\n"
            + "    write(10)\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testNoStatementAfterLeave() {
        final MagikCheck check = new NoStatementAfterBodyExitCheck();
        final String code = ""
            + "_loop\n"
            + "    _leave\n"
            + "_endloop";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testStatementAfterLeave() {
        final MagikCheck check = new NoStatementAfterBodyExitCheck();
        final String code = ""
            + "_loop\n"
            + "    _leave\n"
            + "    write(10)\n"
            + "_endloop";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

}
