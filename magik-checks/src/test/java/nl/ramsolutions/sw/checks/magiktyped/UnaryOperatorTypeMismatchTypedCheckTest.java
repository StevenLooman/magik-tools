package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for {@link UnaryOperatorTypeMismatchTypedCheck}. */
class UnaryOperatorTypeMismatchTypedCheckTest {

  @Test
  void testCompatibleUnaryOperator() {
    final String code =
        """
        _block
          +:symbol
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_SYMBOL,
            "unary_plus",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_SYMBOL),
            ExpressionResultString.EMPTY));

    final MagikTypedCheck check = new UnaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testIncompatibleUnaryOperator() {
    final String code =
        """
        _block
          +:symbol
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new UnaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
