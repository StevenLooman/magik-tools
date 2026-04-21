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
        """
        _method a.m1()
          ## @return {sw:integer} Result.
          _return 1
        _endmethod
        """,
        """
        _method a.m1()
          ## @return {sw:integer} Result.
          >> 1
        _endmethod
        """,
        """
        _method a.m1()
          ## @return {sw:integer} Result.
          >> _if _true
             _then >> 1
             _endif
        _endmethod
        """,
        """
        _method a.m1()
          ## @return {sw:integer} Result.
          >> _if _true
             _then _return 1
             _endif
        _endmethod
        """,
        """
        _method a.m1()
          ## @return {sw:integer} Result.
          _if _true
          _then _return 1
          _endif
        _endmethod
        """,
        """
        _method a.m1()
          a << _if b = c
               _then >> 1
               _endif
        _endmethod
        """,
        """
        _method a.m1()
          _proc()
            ## @return {sw:integer} Result.
            _return 1
          _endproc
        _endmethod
        """,
        """
        _method a.m1()
          ## @return {sw:unset} Result.
          _return _unset
        _endmethod
        """,
        """
        _proc()
          ## @return {sw:integer} Result.
          >> 1
        _endproc
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
        """
        _method a.m1()
          _return 1
        _endmethod
        """,
        """
        _method a.m1()
          >> 1
        _endmethod
        """,
        """
        _method a.m1()
          >> _if _true
             _then >> 1
             _endif
        _endmethod
        """,
        """
        _method a.m1()
          >> _if _true
             _then _return 1
             _endif
        _endmethod
        """,
        """
        _method a.m1()
          _if _true
          _then _return 1
          _endif
        _endmethod
        """,
        """
        _method a.m1()
          _proc()
            _return 1
          _endproc
        _endmethod
        """,
        """
        _method a.m1()
          _return _unset
        _endmethod
        """,
        """
        _proc()
          >> 1
        _endproc
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new TypeDocCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
