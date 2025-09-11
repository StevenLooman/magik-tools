package nl.ramsolutions.sw.magik.typedchecks.checks;

import static nl.ramsolutions.sw.magik.typedchecks.checks.MagikTypedCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

/** Tests for {@link MethodReturnTypesMatchDocTypedCheck}. */
class MethodReturnTypesMatchDocTypedCheckTest {

  @Test
  void testTypesMatches() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          _return 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MethodReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testTypesDiffer() {
    final String code =
        """
        _method a.b
          ## @return {float}
          _return 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MethodReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testIgnoreAbstractMethod() {
    final String code =
        """
        _abstract _method a.b
          ## @return {integer}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MethodReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
