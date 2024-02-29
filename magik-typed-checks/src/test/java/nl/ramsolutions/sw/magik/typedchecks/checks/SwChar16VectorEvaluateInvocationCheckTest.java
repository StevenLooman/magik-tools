package nl.ramsolutions.sw.magik.typedchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

class SwChar16VectorEvaluateInvocationCheckTest extends MagikTypedCheckTestBase {

  @Test
  void testUseOfSwChar16VectorEvaluate() {
    final String code = "'abc'.evaluate()";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new SwChar16VectorEvaluateInvocationCheck();
    final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
    assertThat(checkResults).hasSize(1);
  }

  @Test
  void testUseOfOtherEvaluate() {
    final String code = "100.evaluate()";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new SwChar16VectorEvaluateInvocationCheck();
    final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
    assertThat(checkResults).isEmpty();
  }
}
