package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for {@link BinaryOperatorTypeMismatchTypedCheck}. */
class BinaryOperatorTypeMismatchTypedCheckTest {

  @Test
  void testCompatibleAugmentedAssignmentTypes() {
    final String code =
        """
        _method a.b
          _local x << 1
          x +<< 2
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new BinaryOperatorDefinition(
            null,
            null,
            null,
            null,
            null,
            "+",
            TypeString.SW_INTEGER,
            TypeString.SW_INTEGER,
            TypeString.SW_INTEGER));
    final MagikTypedCheck check = new BinaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testIncompatibleAugmentedAssignmentTypes() {
    final String code =
        """
        _method a.b
          _local x << :symbol
          x +<< 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    // No binary operator defined for symbol + integer.
    final MagikTypedCheck check = new BinaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testCompatibleRegularBinaryExpressionTypes() {
    final String code =
        """
        _method a.b
          _local x << 1 + 2
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new BinaryOperatorDefinition(
            null,
            null,
            null,
            null,
            null,
            "+",
            TypeString.SW_INTEGER,
            TypeString.SW_INTEGER,
            TypeString.SW_INTEGER));
    final MagikTypedCheck check = new BinaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testIncompatibleRegularBinaryExpressionTypes() {
    final String code =
        """
        _method a.b
          _local x << :symbol + 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new BinaryOperatorTypeMismatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
