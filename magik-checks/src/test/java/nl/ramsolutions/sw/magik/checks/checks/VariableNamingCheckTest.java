package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test VariableNamingCheck.
 */
class VariableNamingCheckTest extends MagikCheckTestBase {

    @Test
    void testValidName() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "_local coord";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testValidNameAssignment() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "coord << 10";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testInvalidName() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "_local c";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testInvalidNameAssignment() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "c << 10";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testWhitelistedName() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "_local x";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testValidNameParameter() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "_method a.b(coord) _endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testInvalidNameParameter() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "_method a.b(c) _endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testPrefixedValidName() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "_local l_coord";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testPrefixedInvalidName() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "_local l_c";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testWhitelistedPrefixedName() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "_local l_x";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testMultiVariableDeclarationValidName() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "_local (l_item, l_result) << (1, 2)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testMultiVariableDeclarationInvalidName() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "_local (l_i, l_r) << (1, 2)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

    @Test
    void testAugmentedAssignment() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "result +<< 10";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testAugmentedAssignmentMulti() {
        final MagikCheck check = new VariableNamingCheck();
        final String code = "result +<< str << _self.a";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
