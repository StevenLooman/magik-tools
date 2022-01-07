package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for NewDocCheck.
 */
class NewDocCheckTest extends MagikCheckTestBase {

    @Test
    void testParameterMissing() {
        final String code = ""
            + "_method a.m1(p1)\n"
            + "_endmethod";
        final MagikCheck check = new NewDocCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testParameterUnknown() {
        final String code = ""
            + "_method a.m1()\n"
            + "  ## @param {sw:float} p1 Paramter 1.\n"
            + "_endmethod";
        final MagikCheck check = new NewDocCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testParameterOk() {
        final String code = ""
            + "_method a.m1(p1)\n"
            + "  ## @param {sw:float} p1 Paramter 1.\n"
            + "_endmethod";
        final MagikCheck check = new NewDocCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testSlotMissing() {
        final String code = ""
            + "def_slotted_exemplar(\n"
            + "  :test_exemplar,\n"
            + "  {{:slot1, _unset}})";
        final MagikCheck check = new NewDocCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testSlotUnknown() {
        final String code = ""
            + "## @slot {sw:rope} slot1 Slot 1.\n"
            + "def_slotted_exemplar(\n"
            + "  :test_exemplar,\n"
            + "  {})";
        final MagikCheck check = new NewDocCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testSlotOk() {
        final String code = ""
            + "## @slot {sw:rope} slot1 Slot 1.\n"
            + "def_slotted_exemplar(\n"
            + "  :test_exemplar,\n"
            + "  {{:slot1, _unset}})";
        final MagikCheck check = new NewDocCheck();
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
