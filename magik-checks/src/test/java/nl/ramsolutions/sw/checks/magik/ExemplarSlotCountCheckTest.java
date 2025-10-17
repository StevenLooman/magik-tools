package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link ExemplarSlotCountCheck}. */
class ExemplarSlotCountCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        def_slotted_exemplar(:exemplar, {
          {:slot_1, _unset},
          {:slot_2, _unset},
          {:slot_3, _unset}})
        """,
        """
        sw:def_slotted_exemplar(:exemplar, {
          {:slot_1, _unset},
          {:slot_2, _unset},
          {:slot_3, _unset}})
        """,
      })
  void testMaxSlotCountExceeded(final String code) {
    final ExemplarSlotCountCheck check = new ExemplarSlotCountCheck();
    check.maxSlotCount = 2;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testMaxSlotCountSatisfied() {
    final ExemplarSlotCountCheck check = new ExemplarSlotCountCheck();
    check.maxSlotCount = 2;
    final String code =
        """
        def_slotted_exemplar(:exemplar, {
          {:slot_1, _unset}})
        """;
    assertThat(check).reportsNoIssues(code);
  }
}
