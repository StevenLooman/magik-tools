package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.junit.jupiter.api.Test;

/** Tests for {@link SwChar16VectorEvaluateInvocationTypedCheck}. */
class SwChar16VectorEvaluateInvocationTypedCheckTest {

  @Test
  void testUseOfSwChar16VectorEvaluate() {
    final String code = "'abc'.evaluate()";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new SwChar16VectorEvaluateInvocationTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testUseOfOtherEvaluate() {
    final String code = "100.evaluate()";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new SwChar16VectorEvaluateInvocationTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
