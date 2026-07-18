package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link AssignedTypeDoesNotMatchSlotTypeTypedCheck}. */
class AssignedTypeDoesNotMatchSlotTypeTypedCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
          _method ex.m()
            .slot << 10
          _endmethod
        """,
      })
  void testTypeMatches(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("ex", "user"),
            null));
    definitionKeeper.add(
        new SlotDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.ofIdentifier("ex", "user"),
            "slot",
            TypeString.SW_INTEGER));
    final MagikTypedCheck check = new AssignedTypeDoesNotMatchSlotTypeTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // Slot assignment outside a method definition should not crash.
        ".slot << 10",
      })
  void testSlotAssignmentOutsideMethod(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new AssignedTypeDoesNotMatchSlotTypeTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
          _method ex.m()
            .slot << {}
          _endmethod
        """,
      })
  void testTypeNotMatches(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final SlotDefinition slotDefinition =
        new SlotDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.ofIdentifier("ex", "user"),
            "slot",
            TypeString.ofIdentifier("sw", "integer"));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("ex", "user"),
            null));
    definitionKeeper.add(slotDefinition);
    final MagikTypedCheck check = new AssignedTypeDoesNotMatchSlotTypeTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
