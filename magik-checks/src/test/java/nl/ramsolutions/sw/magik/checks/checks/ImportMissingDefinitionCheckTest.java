package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ImportMissingDefinitionCheck.
 */
class ImportMissingDefinitionCheckTest extends MagikCheckTestBase {

    @Test
    void testImportOkLocal() {
        final MagikCheck check = new ImportMissingDefinitionCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _local var\n"
            + "    _proc()\n"
            + "        _import var\n"
            + "    _endproc\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testImportOkLocal2() {
        final MagikCheck check = new ImportMissingDefinitionCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _if _true\n"
            + "    _then\n"
            + "        _local var\n"
            + "        _proc()\n"
            + "            _import var\n"
            + "        _endproc\n"
            + "    _endif\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testImportOkConstant() {
        final MagikCheck check = new ImportMissingDefinitionCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _constant const\n"
            + "    _proc()\n"
            + "        _import const\n"
            + "    _endproc\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testImportOkParameter() {
        final MagikCheck check = new ImportMissingDefinitionCheck();
        final String code = ""
            + "_method a.b(param)\n"
            + "    _proc()\n"
            + "        _import param\n"
            + "    _endproc\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testImportOkIter() {
        final MagikCheck check = new ImportMissingDefinitionCheck();
        final String code = ""
            + "_method a.b(param)\n"
            + "    _for i _over 1.upto(5)\n"
            + "    _loop\n"
            + "        _proc()\n"
            + "            _import i\n"
            + "        _endproc\n"
            + "    _endloop\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testImportFailMissing() {
        final MagikCheck check = new ImportMissingDefinitionCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _proc()\n"
            + "        _import var\n"
            + "    _endproc\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testImportFailNonLocal() {
        final MagikCheck check = new ImportMissingDefinitionCheck();
        final String code = ""
            + "_method a.b\n"
            + "    var << 1\n"
            + "    _proc()\n"
            + "        _import var\n"
            + "    _endproc\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

}
