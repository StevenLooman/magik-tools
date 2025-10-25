package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link MethodArgumentTypeMatchesParameterTypeTypedCheck}. */
class MethodArgumentTypeMatchesParameterTypeTypedCheckTest {

  private void addTestMethods(final IDefinitionKeeper definitionKeeper) {
    definitionKeeper.add(
        new MethodDefinition(
            null,
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
                    null,
                    "p1",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_SYMBOL)),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "m2()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            List.of(
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "p1",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.combine(TypeString.SW_SYMBOL, TypeString.SW_UNSET))),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "integer.m1(:symbol)",
        "integer.m1()", // No argument, nothing to check.
        "integer.m1(:symbol, :symbol)", // We only test type, not number of arguments.
        "integer.m2(:symbol)",
      })
  void testArgumentTypeMatches(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addTestMethods(definitionKeeper);

    final MagikTypedCheck check = new MethodArgumentTypeMatchesParameterTypeTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "integer.m1(1)",
        "integer.m2(1)",
      })
  void testArgumentTypeNotMatches(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addTestMethods(definitionKeeper);

    final MagikTypedCheck check = new MethodArgumentTypeMatchesParameterTypeTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testGenericSubstitution() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier(
                "rope", "sw", TypeString.ofGenericDefinition("E", TypeString.SW_INTEGER)),
            Collections.emptyList(),
            Collections.emptyList(),
            null));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.ofIdentifier("rope", "sw"),
            "add()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            List.of(
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "thing",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.ofGenericReference("E"))),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final String code =
        """
        _block
          _local ints << rope  # type sw:rope<E=sw:integer>
          ints.add(10)
        _endblock
        """;
    final MagikTypedCheck check = new MethodArgumentTypeMatchesParameterTypeTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
