package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test UnusedVariableCheck.
 */
class UnusedVariableCheckTest extends MagikCheckTestBase {

    @Test
    void testVariableUsed() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _local a\n"
            + "    print(a)\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testVariableNotUsed() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _local a\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testVariableNotUsedForLoop() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _for a _over x.fast_elements()\n"
            + "    _loop\n"
            + "    _endloop\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testVariableNotUsedMultiAssignment() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _local (a, b) << (_scatter {1,2})\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

    @Test
    void testVariableFirstNotUsedMultiVariableDefinition() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _local (a, b) << (_scatter {1,2})\n"
            + "    write(b)\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testVariableFirstNotUsedMultiAssignment() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    (a, b) << (_scatter {1,2})\n"
            + "    write(b)\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testVariableFirstNotUsedForLoop() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _for a, b _over x.fast_keys_and_elements()\n"
            + "    _loop\n"
            + "        write(b)\n"
            + "    _endloop\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testDynamicAssigned() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _dynamic !notify_database_data_changes?! << _false\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testDynamicAssignedLater() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _dynamic !notify_database_data_changes?!\n"
            + "    !notify_database_data_changes?! << _false\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testDynamicNotAssigned() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _dynamic !notify_database_data_changes?!\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testAssignmentUsed() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    a << 10\n"
            + "    print(a)\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testAssignmentNotUsed() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    a << 10\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testAssignmentMethod() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b(param1)\n"
            + "    param1.a << 10\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testParameter() {
        final MagikCheck check = new UnusedVariableCheck(true);
        final String code = ""
            + "_method a.b(p_param1)\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testParameterOptional() {
        final MagikCheck check = new UnusedVariableCheck(true);
        final String code = ""
            + "_method a.b(p_param1, _optional p_param2)\n"
            + "    write(p_param1)\n"
            + "    write(p_param2)\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testParameterGather() {
        final MagikCheck check = new UnusedVariableCheck(true);
        final String code = ""
            + "_method a.b(p_param1, _gather p_param2)\n"
            + "    write(p_param1)\n"
            + "    write(p_param2)\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testMethodProcedureImport() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _local l_me << _self\n"
            + "    _proc()\n"
            + "        _import l_me\n"
            + "        print(l_me)\n"
            + "    _endproc()\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testMethodProcedureImportAssignment() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _local l_me << _self\n"
            + "    _proc()\n"
            + "        _import l_me\n"
            + "        l_me << 10\n"
            + "    _endproc()\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testCase() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b()\n"
            + "    l_Set << 10\n"
            + "    print(l_set)\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testUndeclaredGlobal() {
        final MagikCheck check = new UnusedVariableCheck();
        final String code = ""
            + "_method a.b()\n"
            + "    _if !current_grs! _isnt _unset\n"
            + "    _then\n"
            + "        write(1)\n"
            + "    _endif\n"
            + "_endmethod\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
