package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link DeprecatedMethodUsageTypedCheck}. */
class DeprecatedMethodUsageTypedCheckTest {

  private void addMethodDefinition(
      final IDefinitionKeeper definitionKeeper, final String... classifyLevel) {
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            new Pragma(null, Arrays.asList(classifyLevel), Set.of(), Set.of()),
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
  }

  @Test
  void testMethodDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addMethodDefinition(definitionKeeper, "deprecated");
    final String code =
        """
        _block
          object.m()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedMethodUsageTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMethodNotDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addMethodDefinition(definitionKeeper, "not_deprecated");
    final String code =
        """
        _block
          object.m()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedMethodUsageTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
