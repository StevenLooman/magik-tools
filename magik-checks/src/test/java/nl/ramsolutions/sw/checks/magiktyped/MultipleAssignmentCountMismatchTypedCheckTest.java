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

/** Tests for {@link MultipleAssignmentCountMismatchTypedCheck}. */
class MultipleAssignmentCountMismatchTypedCheckTest {

  @Test
  void testCountMatches() {
    final String code =
        """
        _method a.b
          (x, y) << (1, 2)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MultipleAssignmentCountMismatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testCountMismatchMoreVariables() {
    final String code =
        """
        _method a.b
          (x, y, z) << (1, 2)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MultipleAssignmentCountMismatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testCountMismatchFewerVariables() {
    final String code =
        """
        _method a.b
          (x) << (1, 2)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MultipleAssignmentCountMismatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testCountMatchesFromMethodCall() {
    final String code =
        """
        _block
          (x, y) << object.m()
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
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_INTEGER, TypeString.SW_SYMBOL),
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new MultipleAssignmentCountMismatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testCountMismatchFromMethodCall() {
    final String code =
        """
        _block
          (x, y, z) << object.m()
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
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_INTEGER, TypeString.SW_SYMBOL),
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new MultipleAssignmentCountMismatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testGatherOnLeftSide() {
    final String code =
        """
        _method a.b
          (_gather x) << (1, 2, 3)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MultipleAssignmentCountMismatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testLocalCountMatches() {
    final String code =
        """
        _method a.b
          _local (x, y) << (1, 2)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MultipleAssignmentCountMismatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testLocalCountMismatch() {
    final String code =
        """
        _method a.b
          _local (x, y, z) << (1, 2)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MultipleAssignmentCountMismatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
