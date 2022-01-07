package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test MagikCheckTestBase.
 */
class NoSelfUseCheckTest extends MagikCheckTestBase {

    @Test
    void testSelfUse() {
        final MagikCheck check = new NoSelfUseCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _self.m\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testCloneUse() {
        final MagikCheck check = new NoSelfUseCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _clone.m\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testSuperUse() {
        final MagikCheck check = new NoSelfUseCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _super.m\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testSlotUse() {
        final MagikCheck check = new NoSelfUseCheck();
        final String code = ""
            + "_method a.b\n"
            + "    write(.slot)\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testNoSelfUse() {
        final MagikCheck check = new NoSelfUseCheck();
        final String code = ""
            + "_method a.b\n"
            + "    show(1)\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testNoSelfUseAbstractMethod() {
        final MagikCheck check = new NoSelfUseCheck();
        final String code = ""
            + "_abstract _method a.b\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
