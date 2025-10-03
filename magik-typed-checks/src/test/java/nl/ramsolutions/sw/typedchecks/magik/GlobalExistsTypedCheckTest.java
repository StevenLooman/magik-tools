package nl.ramsolutions.sw.typedchecks.magik;

import static nl.ramsolutions.sw.typedchecks.magik.MagikTypedCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.typedchecks.MagikTypedCheck;
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
