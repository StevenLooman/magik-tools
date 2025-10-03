package nl.ramsolutions.sw.typedchecks.magik;

import static nl.ramsolutions.sw.typedchecks.magik.MagikTypedCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.typedchecks.MagikTypedCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link TypeDocTypeExistsTypedCheck}. */
class TypeDocTypeExistsTypedCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b(p1)
          ## @param {user:missing_type} p1
        _endmethod
        """,
        """
        _method a.b()
          ## @return {user:missing_type}
        _endmethod
        """,
        """
        _method a.b()
          ## @return {|sw:float} p1
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b(p1)
          ## @param {sw:float} p1
        _endmethod
        """,
        """
        _method a.b()
          ## @return {sw:float|sw:integer}
        _endmethod
        """,
      })
  void testValid(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
