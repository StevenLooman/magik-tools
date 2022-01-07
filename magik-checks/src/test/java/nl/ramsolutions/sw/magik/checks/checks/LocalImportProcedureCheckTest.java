package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test LocalImportProcedureCheck.
 */
class LocalImportProcedureCheckTest extends MagikCheckTestBase {

    @Test
    void testImportOk() {
        final MagikCheck check = new LocalImportProcedureCheck();
        final String code = ""
            + "_method a.a\n"
            + "    _local x\n"
            + "    _proc()\n"
            + "        _import x\n"
            + "        x.do()\n"
            + "    _endproc\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testLocalButMeantImport() {
        final MagikCheck check = new LocalImportProcedureCheck();
        final String code = ""
            + "_method a.a\n"
            + "    _local x\n"
            + "    _proc()\n"
            + "        _local x\n"
            + "        x.do()\n"
            + "    _endproc\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMethodProcedureParameter() {
        final MagikCheck check = new LocalImportProcedureCheck();
        final String code = ""
            + "_method a.a(p_a)\n"
            + "    _proc(p_a)\n"
            + "    _endproc\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testTry() {
        final MagikCheck check = new LocalImportProcedureCheck();
        final String code = ""
            + "_try _with a\n"
            + "_when error\n"
            + "_endtry\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testSyntaxError() {
        final MagikCheck check = new LocalImportProcedureCheck();
        final String code = ""
            + "_proc()\n"
            + "  _error\n"
            + "_endproc\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
