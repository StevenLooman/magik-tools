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
import org.junit.jupiter.api.Test;

/** Test {@link SlotExistsTypedCheck}. */
class SlotExistsTypedCheckTest {

  @Test
  void testTypeUnknown() {
    final String code =
        """
        _method ex.m()
          .slot << 10
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new SlotExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testSlotUnknown() {
    final String code =
        """
        _method object.m()
          .slot << 10
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new SlotExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testSlotKnown() {
    final String code =
        """
        _method a.m()
          .slot << 10
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString aRef = TypeString.ofIdentifier("a", "sw");
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            aRef,
            List.of(new SlotDefinition(null, null, null, null, null, "slot", TypeString.UNDEFINED)),
            Collections.emptyList(),
            null));
    final MagikTypedCheck check = new SlotExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
