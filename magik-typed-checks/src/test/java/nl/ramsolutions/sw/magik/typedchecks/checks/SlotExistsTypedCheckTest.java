package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
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
        final ITypeKeeper typeKeeper = new TypeKeeper();

        final String code = ""
            + "_method ex.m()\n"
            + "    .slot << 10\n"
            + "_endmethod";
        final MagikTypedCheck check = new SlotExistsTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testSlotUnknown() {
        final ITypeKeeper typeKeeper = new TypeKeeper();

        final String code = ""
            + "_method object.m()\n"
            + "    .slot << 10\n"
            + "_endmethod";
        final MagikTypedCheck check = new SlotExistsTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testSlotKnown() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString objectTypeStr = TypeString.ofIdentifier("object", "sw");
        final MagikType objectType = (MagikType) typeKeeper.getType(objectTypeStr);
        objectType.addSlot(null, "slot", TypeString.UNDEFINED);

        final String code = ""
            + "_method object.m()\n"
            + "    .slot << 10\n"
            + "_endmethod";
        final MagikTypedCheck check = new SlotExistsTypedCheck();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).isEmpty();
    }

}
