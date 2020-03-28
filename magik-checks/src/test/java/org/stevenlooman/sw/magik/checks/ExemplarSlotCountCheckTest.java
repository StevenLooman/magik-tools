package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class ExemplarSlotCountCheckTest extends MagikCheckTestBase {

  @Test
  public void testMaxSlotCountExceeded() {
    ExemplarSlotCountCheck check = new ExemplarSlotCountCheck();
    check.maxSlotCount = 2;
    String code =
        "def_slotted_exemplar(:exemplar, {" +
            "{:slot_1, _unset}," +
            "{:slot_2, _unset}," +
            "{:slot_3, _unset}})\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testMaxSlotCountSatisfied() {
    ExemplarSlotCountCheck check = new ExemplarSlotCountCheck();
    check.maxSlotCount = 2;
    String code =
        "def_slotted_exemplar(:exemplar, {" +
            "{:slot_1, _unset}})\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testVec() {
    ExemplarSlotCountCheck check = new ExemplarSlotCountCheck();
    check.maxSlotCount = 2;
    String code =
        "def_slotted_exemplar(:exemplar, vec(" +
            "vec(:slot_1, _unset)))\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

}
