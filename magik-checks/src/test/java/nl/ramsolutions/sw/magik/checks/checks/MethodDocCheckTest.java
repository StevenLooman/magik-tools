package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test MethodDocCheck.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class MethodDocCheckTest extends MagikCheckTestBase {

    @Test
    void testMethodDoc() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a.b(param1, param2)\n"
            + "    ## Function  : example\n"
            + "    ##             multi-line\n"
            + "    ## Parameters: PARAM1: example parameter 1\n"
            + "    ##             PARAM2: example parameter 2\n"
            + "    ## Returns   : -\n"
            + "    do_something()\n"
            + "    _if a\n"
            + "    _then\n"
            + "        write(a)\n"
            + "    _endif\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testMethodDocMissing() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a.b(param1)\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(3);
    }

    @Test
    void testMethodDocMissingFunction() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a.b(param1)\n"
            + "    ## Parameters: PARAM1: example parameters\n"
            + "    ## Returns: -\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMethodDocMissingParameters() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a.b(param1)\n"
            + "    ## Function: example\n"
            + "    ## Returns: -\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMethodDocMissingReturns() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a.b(param1)\n"
            + "    ## Function: example\n"
            + "    ## Parameters: PARAM1: example parameters\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMethodDocMissingParametersSingle() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a.b(param1, param2)\n"
            + "    ## Function: example\n"
            + "    ## Parameters: PARAM1: example parameters\n"
            + "    ## Returns: -\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMethodDocMissingParametersOptional() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a.b(param1, _optional param2)\n"
            + "    ## Function: example\n"
            + "    ## Parameters: PARAM1: example parameters\n"
            + "    ## Returns: -\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMethodDocParametersAssignment() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a.b << value\n"
            + "    ## Function: example\n"
            + "    ## Parameters: VALUE: new value\n"
            + "    ## Returns: -\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testMethodDocParametersAssignmentMissing() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a.b << value\n"
            + "    ## Function: example\n"
            + "    ## Parameters: -\n"
            + "    ## Returns: -\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMethodDocParametersIndex() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a[index]\n"
            + "    ## Function: example\n"
            + "    ## Parameters: INDEX: index\n"
            + "    ## Returns: -\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testMethodDocParametersIndexMissing() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a[index]\n"
            + "    ## Function: example\n"
            + "    ## Parameters: -\n"
            + "    ## Returns: -\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testMethodDocParametersIndexAssignment() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a[index] << value\n"
            + "    ## Function: example\n"
            + "    ## Parameters: INDEX: index\n"
            + "    ##             VALUE: value\n"
            + "    ## Returns: -\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testMethodDocParametersOptional() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a.b(_optional param1)\n"
            + "    ## Function: example\n"
            + "    ## Parameters: PARAM1: parameter 1\n"
            + "    ## Returns: -\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testMethodDocParametersIndexAssignmentMissing() {
        final MagikCheck check = new MethodDocCheck();
        final String code = ""
            + "_method a[index] << value\n"
            + "    ## Function: example\n"
            + "    ## Parameters: -\n"
            + "    ## Returns: -\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

}
