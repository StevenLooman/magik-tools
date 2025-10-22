package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link TypeDocCheck}. */
class TypeDocCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.m1(p1)
          ## @param {sw:float} p1 Parameter 1.
        _endmethod
        """,
        """
        ## @slot {sw:rope} slot1 Slot 1.
        def_slotted_exemplar(
          :test_exemplar,
          {{:slot1, _unset}})
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new TypeDocCheck();
    assertThat(check).reportsNoIssues(code);
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
          ## @param {sw:float} p1 Parameter 1.
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
      })
  void testInvalid(final String code) {
    final MagikCheck check = new TypeDocCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
