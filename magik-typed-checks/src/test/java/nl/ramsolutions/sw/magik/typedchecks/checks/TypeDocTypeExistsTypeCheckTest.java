package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TypeDocTypeExistsTypeCheck.
 */
class TypeDocTypeExistsTypeCheckTest extends MagikTypedCheckTestBase {

    @Test
    void testParameterUnknownType() {
        final String code = ""
            + "_method a.b(p1)\n"
            + "  ## @param {user:missing_type} p1\n"
            + "_endmethod";
        final MagikTypedCheck check = new TypeDocTypeExistsTypeCheck();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testParameterKnownType() {
        final String code = ""
            + "_method a.b(p1)\n"
            + "  ## @param {sw:float} p1\n"
            + "_endmethod";
        final MagikTypedCheck check = new TypeDocTypeExistsTypeCheck();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testReturnUnknownType() {
        final String code = ""
            + "_method a.b()\n"
            + "  ## @return {user:missing_type}\n"
            + "_endmethod";
        final MagikTypedCheck check = new TypeDocTypeExistsTypeCheck();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testReturnKnownType() {
        final String code = ""
            + "_method a.b()\n"
            + "  ## @return {sw:float}\n"
            + "_endmethod";
        final MagikTypedCheck check = new TypeDocTypeExistsTypeCheck();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testReturnSyntaxError() {
        final String code = ""
            + "_method a.b()\n"
            + "  ## @return {|sw:float} p1\n"
            + "_endmethod";
        final MagikTypedCheck check = new TypeDocTypeExistsTypeCheck();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).hasSize(1);
    }

}
