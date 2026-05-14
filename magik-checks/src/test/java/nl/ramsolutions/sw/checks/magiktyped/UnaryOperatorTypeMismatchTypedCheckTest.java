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

  @Test
  void testIncompatibleScatterUnaryOperatorWithoutForScatterDefinition() {
    final String code =
        """
        _block
          _scatter {:symbol}
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final MagikTypedCheck check = new UnaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testCompatibleScatterUnaryOperatorWithForScatterReturnType() {
    final String code =
        """
        _block
          _scatter {:symbol}
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
            TypeString.SW_SIMPLE_VECTOR,
            "for_scatter()",
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
  void testCompatibleScatterOnGatherParameterWithForScatterReturnType() {
    final String code =
        """
        _block
          _method test.write_line(_gather parts)
            ## @param {sw:char16_vector} parts
            _scatter parts
          _endmethod
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
            TypeString.SW_SIMPLE_VECTOR,
            "for_scatter()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_OBJECT),
            ExpressionResultString.EMPTY));

    final MagikTypedCheck check = new UnaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testIncompatibleNegatedUnaryOperatorWithoutNegatedDefinition() {
    final String code =
        """
        _block
          -42
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final MagikTypedCheck check = new UnaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testCompatibleNegatedUnaryOperatorWithNegatedReturnType() {
    final String code =
        """
        _block
          -42
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
            TypeString.SW_INTEGER,
            "negated",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_INTEGER),
            ExpressionResultString.EMPTY));

    final MagikTypedCheck check = new UnaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testIncompatibleNotUnaryOperatorWithoutNotDefinition() {
    final String code =
        """
        _block
          _not _true
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final MagikTypedCheck check = new UnaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testCompatibleNotUnaryOperatorWithNotReturnType() {
    final String code =
        """
        _block
          _not _true
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
            TypeString.SW_FALSE,
            "not",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_FALSE),
            ExpressionResultString.EMPTY));

    final MagikTypedCheck check = new UnaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
