package nl.ramsolutions.sw.magik.typedchecks.checks;

import static nl.ramsolutions.sw.magik.typedchecks.checks.MagikTypedCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
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
            List.of(
                new SlotDefinition(null, null, null, null, null, "slot", TypeString.SW_INTEGER)),
            Collections.emptyList(),
            null));
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
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("ex", "user"),
            List.of(
                new SlotDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "slot",
                    TypeString.ofIdentifier("sw", "integer"))),
            Collections.emptyList(),
            null));
    final MagikTypedCheck check = new AssignedTypeDoesNotMatchSlotTypeTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
