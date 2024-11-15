package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for TypeDocCheck. */
class TypeDocCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
    _method a.m1(p1)
      ## @param {sw:float} p1 Paramter 1.
    _endmethod
    """,
        """
    ## @slot {sw:rope} slot1 Slot 1.
    def_slotted_exemplar(
      :test_exemplar,
      {{:slot1, _unset}})
    """,
        """
    _proc(a)
      ## @param {sw:float} a
    _endproc
    """,
      })
  void testValid(final String code) {
    final MagikCheck check = new TypeDocCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
    _method a.m1(p1)
    _endmethod
    """,
        """
    _method a.m1()
      ## @param {sw:float} p1 Paramter 1.
    _endmethod
    """,
        """
    def_slotted_exemplar(
      :test_exemplar,
      {{:slot1, _unset}})
    """,
        """
    ## @slot {sw:rope} slot1 Slot 1.
    def_slotted_exemplar(
      :test_exemplar,
      {})
    """,
        """
    _proc(a)
    _endproc
    """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new TypeDocCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testProcedureIsNotCheckedIfCheckProceduresIsFalse() {
    final TypeDocCheck check = new TypeDocCheck();
    check.checkProcedures = false;
    final String code =
        """
        _proc(a)
        _endproc
        """;
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
