package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link TypeDocMixedGenericsCheck}. */
class TypeDocMixedGenericsCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.m1()
          ## @return {sw:rope<E=sw:symbol>} Result.
        _endmethod
        """,
        """
        _method a.m1()
          ## @return {sw:property_list<K=sw:symbol, E=sw:object>} Result.
        _endmethod
        """,
        """
        _method a.m1()
          ## @return {sw:rope<E>} Result.
        _endmethod
        """,
        """
        _method a.m1()
          ## @return {sw:integer} Result.
        _endmethod
        """,
      })
  void testValid(final String code) {
    final MagikCheck check = new TypeDocMixedGenericsCheck();
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.m1()
          ## @return {sw:property_list<K=sw:symbol,object>} Result.
        _endmethod
        """,
        """
        _method a.m1()
          ## @param {sw:property_list<K=sw:symbol,object>} p1 Param.
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final MagikCheck check = new TypeDocMixedGenericsCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
