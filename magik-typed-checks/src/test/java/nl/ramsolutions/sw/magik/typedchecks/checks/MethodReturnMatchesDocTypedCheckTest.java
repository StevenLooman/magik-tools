package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MethodReturnMatchesDocCheck.
 */
class MethodReturnMatchesDocTypedCheckTest extends MagikTypedCheckTestBase {

    @Test
    void testTypesMatches() {
        final String code = ""
            + "_method a.b\n"
            + "  ## @return {integer}\n"
            + "  _return 1\n"
            + "_endmethod";
        final MagikTypedCheck check = new MethodReturnMatchesDocTypedCheck();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testTypesDiffer() {
        final String code = ""
            + "_method a.b\n"
            + "  ## @return {float}\n"
            + "  _return 1\n"
            + "_endmethod";
        final MagikTypedCheck check = new MethodReturnMatchesDocTypedCheck();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testIgnoreAbstractMethod() {
        final String code = ""
            + "_abstract _method a.b\n"
            + "  ## @return {integer}\n"
            + "_endmethod";
        final MagikTypedCheck check = new MethodReturnMatchesDocTypedCheck();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).isEmpty();
    }

}
