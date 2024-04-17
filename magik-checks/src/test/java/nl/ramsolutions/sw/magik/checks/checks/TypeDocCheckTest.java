package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

/** Tests for TypeDocCheck. */
class TypeDocCheckTest extends MagikCheckTestBase {

  @Test
  void testParameterMissing() {
    final String code =
        """
        _method a.m1(p1)
        _endmethod""";
    final MagikCheck check = new TypeDocCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testParameterUnknown() {
    final String code =
        """
        _method a.m1()
          ## @param {sw:float} p1 Paramter 1.
        _endmethod""";
    final MagikCheck check = new TypeDocCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testParameterOk() {
    final String code =
        """
        _method a.m1(p1)
          ## @param {sw:float} p1 Paramter 1.
        _endmethod""";
    final MagikCheck check = new TypeDocCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testSlotMissing() {
    final String code =
        """
        def_slotted_exemplar(
          :test_exemplar,
          {{:slot1, _unset}})""";
    final MagikCheck check = new TypeDocCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testSlotUnknown() {
    final String code =
        """
        ## @slot {sw:rope} slot1 Slot 1.
        def_slotted_exemplar(
          :test_exemplar,
          {})""";
    final MagikCheck check = new TypeDocCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testSlotOk() {
    final String code =
        """
        ## @slot {sw:rope} slot1 Slot 1.
        def_slotted_exemplar(
          :test_exemplar,
          {{:slot1, _unset}})""";
    final MagikCheck check = new TypeDocCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
