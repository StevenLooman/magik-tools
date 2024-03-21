package nl.ramsolutions.sw.magik.typedchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link MethodArgumentTypeMatchesParameterTypeTypedCheck}. */
class MethodArgumentTypeMatchesParameterTypeTypedCheckTest extends MagikTypedCheckTestBase {

  private void addTestMethod(final IDefinitionKeeper definitionKeeper) {
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "m1()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            List.of(
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    "p1",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_SYMBOL)),
            null,
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "integer.m1(:symbol)",
        "integer.m1()", // No argument, nothing to check.
        "integer.m1(:symbol, :symbol)", // We only test type, not number of arguments.
      })
  void testValid(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addTestMethod(definitionKeeper);

    final MagikTypedCheck check = new MethodArgumentTypeMatchesParameterTypeTypedCheck();
    final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
    assertThat(checkResults).isEmpty();
  }

  @Test
  void testArgumentTypeNotMatches() {
    final String code = "integer.m1(1)";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addTestMethod(definitionKeeper);

    final MagikTypedCheck check = new MethodArgumentTypeMatchesParameterTypeTypedCheck();
    final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
    assertThat(checkResults).hasSize(1);
  }
}
