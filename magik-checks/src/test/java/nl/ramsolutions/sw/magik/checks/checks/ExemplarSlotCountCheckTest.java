package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Test ExemplarSlotCountCheck. */
class ExemplarSlotCountCheckTest extends MagikCheckTestBase {

  @Test
  void testMaxSlotCountExceeded() {
    final ExemplarSlotCountCheck check = new ExemplarSlotCountCheck();
    check.maxSlotCount = 2;
    final String code =
        ""
            + "def_slotted_exemplar(:exemplar, {\n"
            + "  {:slot_1, _unset},\n"
            + "  {:slot_2, _unset},\n"
            + "  {:slot_3, _unset}})\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMaxSlotCountSatisfied() {
    final ExemplarSlotCountCheck check = new ExemplarSlotCountCheck();
    check.maxSlotCount = 2;
    final String code = "" + "def_slotted_exemplar(:exemplar, {\n" + "    {:slot_1, _unset}})\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testMaxSlotCountExceededPackage() {
    final ExemplarSlotCountCheck check = new ExemplarSlotCountCheck();
    check.maxSlotCount = 2;
    final String code =
        ""
            + "sw:def_slotted_exemplar(:exemplar, {\n"
            + "  {:slot_1, _unset},\n"
            + "  {:slot_2, _unset},\n"
            + "  {:slot_3, _unset}})\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }
}
