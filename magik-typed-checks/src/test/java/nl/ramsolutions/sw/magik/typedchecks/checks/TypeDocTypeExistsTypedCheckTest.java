package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TypeDocTypeExistsTypeCheck.
 */
class TypeDocTypeExistsTypedCheckTest extends MagikTypedCheckTestBase {

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.b(p1)\n"
            + "  ## @param {user:missing_type} p1\n"
            + "_endmethod",
        ""
            + "_method a.b()\n"
            + "  ## @return {user:missing_type}\n"
            + "_endmethod",
        ""
            + "_method a.b()\n"
            + "  ## @return {|sw:float} p1\n"
            + "_endmethod",
    })
    void testInvalid(final String code) {
        final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.b(p1)\n"
            + "  ## @param {sw:float} p1\n"
            + "_endmethod",
        ""
            + "_method a.b()\n"
            + "  ## @return {sw:float}\n"
            + "_endmethod",
    })
    void testValid(final String code) {
        final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final List<MagikIssue> issues = this.runCheck(code, typeKeeper, check);
        assertThat(issues).isEmpty();
    }

}
