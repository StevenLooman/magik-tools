package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.junit.jupiter.api.Test;

/** Test {@link GlobalExistsTypedCheck}. */
class GlobalExistsTypedCheckTest {

  @Test
  void testKnownGlobal() {
    final String code = "float.m";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final MagikTypedCheck check = new GlobalExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testUnknownGlobal() {
    final String code = "abc.m";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final MagikTypedCheck check = new GlobalExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
