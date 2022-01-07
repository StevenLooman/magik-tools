package nl.ramsolutions.sw.magik.checks.checks;

import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test CommentedCodeCheck.
 */
class CommentedCodeCheckTest extends MagikCheckTestBase {

    @Test
    void testNoCommentedCode() {
        final MagikCheck check = new CommentedCodeCheck();
        final String code = ""
            + "_method a.b\n"
            + "    _local x << _self.call()\n"
            + "    x +<< 10\n"
            + "    write(x)\n"
            + "    _return x\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testCommentedCode() {
        final MagikCheck check = new CommentedCodeCheck();
        final String code = ""
            + "_method a.b\n"
            + "    #_local x << _self.call()\n"
            + "    #x +<< 10\n"
            + "    #write(x)\n"
            + "    #_return x\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(1);
    }

    @Test
    void testCommentedCodeTwice() {
        final MagikCheck check = new CommentedCodeCheck();
        final String code = ""
            + "_method a.b\n"
            + "    #_local x << _self.call()\n"
            + "    #x +<< 10\n"
            + "    #write(x)\n"
            + "    #_return x\n"
            + "    _return 10\n"
            + "    #_local x << _self.call()\n"
            + "    #x +<< 10\n"
            + "    #write(x)\n"
            + "    #_return x\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).hasSize(2);
    }

    @Test
    void testCommentedMessage() {
        final MagikCheck check = new CommentedCodeCheck();
        final String code = ""
            + "_method a.b\n"
            + "    # This is\n"
            + "    # just a\n"
            + "    # message,\n"
            + "    # no real.\n"
            + "    # code.\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testCommentsAfterCodeIgnored() {
        final MagikCheck check = new CommentedCodeCheck();
        final String code = ""
            + "_method a.b\n"
            + "    a # z\n"
            + "    b # y\n"
            + "    c # x\n"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testIgnoreMehtodDoc() {
        final CommentedCodeCheck check = new CommentedCodeCheck();
        check.minLines = 2;
        final String code = ""
            + "_method a.b\n"
            + "    ## Interesting\n"
            + "    ## story.\n"
            + "    # x\n"
            + "    print(1)"
            + "_endmethod";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

}
