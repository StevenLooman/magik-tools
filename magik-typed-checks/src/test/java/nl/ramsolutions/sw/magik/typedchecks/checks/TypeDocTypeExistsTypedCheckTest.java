package nl.ramsolutions.sw.magik.typedchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link TypeDocTypeExistsTypedCheck}. */
class TypeDocTypeExistsTypedCheckTest extends MagikTypedCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b(p1)
          ## @param {user:missing_type} p1
        _endmethod""",
        """
        _method a.b()
          ## @return {user:missing_type}
        _endmethod""",
        """
        _method a.b()
          ## @return {|sw:float} p1
        _endmethod""",
      })
  void testInvalid(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b(p1)
          ## @param {sw:float} p1
        _endmethod""",
        """
        _method a.b()
          ## @return {sw:float|sw:integer}
        _endmethod""",
      })
  void testValid(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).isEmpty();
  }
}
