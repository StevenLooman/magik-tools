package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test SlotExistsTypedCheck.
 */
class SlotExistsTypedCheckTest extends MagikTypedCheckTestBase {

    @Test
    void testTypeUnknown() {
        final String code = ""
            + "_method ex.m()\n"
            + "    .slot << 10\n"
            + "_endmethod";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedCheck check = new SlotExistsTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testSlotUnknown() {
        final String code = ""
            + "_method object.m()\n"
            + "    .slot << 10\n"
            + "_endmethod";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedCheck check = new SlotExistsTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testSlotKnown() {
        final String code = ""
            + "_method a.m()\n"
            + "    .slot << 10\n"
            + "_endmethod";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString aRef = TypeString.ofIdentifier("a", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                aRef,
                List.of(
                    new SlotDefinition(null, null, null, null, "slot", TypeString.UNDEFINED)),
                Collections.emptyList()));
        final MagikTypedCheck check = new SlotExistsTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
        assertThat(issues).isEmpty();
    }

}
