package nl.ramsolutions.sw.magik.typedchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

/** Tests for {@link MethodReturnTypesMatchDocTypedCheck}. */
class MethodReturnTypesMatchDocTypedCheckTest extends MagikTypedCheckTestBase {

  @Test
  void testTypesMatches() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          _return 1
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MethodReturnTypesMatchDocTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testTypesDiffer() {
    final String code =
        """
        _method a.b
          ## @return {float}
          _return 1
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MethodReturnTypesMatchDocTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testIgnoreAbstractMethod() {
    final String code =
        """
        _abstract _method a.b
          ## @return {integer}
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MethodReturnTypesMatchDocTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).isEmpty();
  }
}
