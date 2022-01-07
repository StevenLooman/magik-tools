package nl.ramsolutions.sw.magik.checks.checks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test FormattingCheck.
 */
class FormattingCheckTest extends MagikCheckTestBase {

    @Test
    void testCommaProper1() {
        final MagikCheck check = new FormattingCheck();
        final String code = "{1, 2}";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testCommaProper2() {
        final MagikCheck check = new FormattingCheck();
        final String code = "{1, :|a|, 2}";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testCommaImproper1() {
        final MagikCheck check = new FormattingCheck();
        final String code = "{1,2}";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testCommaImproper2() {
        final MagikCheck check = new FormattingCheck();
        final String code = "{1 , 2}";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testCommaImproper3() {
        final MagikCheck check = new FormattingCheck();
        final String code = ""
            + "{1 ,\n"
            + " 2}";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testBinaryOperatorProper1() {
        final MagikCheck check = new FormattingCheck();
        final String code = "a * b";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testBinaryOperatorProper2() {
        final MagikCheck check = new FormattingCheck();
        final String code = "a _isnt b";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testBinaryOperatorProper3() {
        final MagikCheck check = new FormattingCheck();
        final String code = "a +<< b";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testBinaryOperatorImproper1() {
        final MagikCheck check = new FormattingCheck();
        final String code = "a*b";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testBinaryOperatorImproper2() {
        final MagikCheck check = new FormattingCheck();
        final String code = "a* b";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testBinaryOperatorImproper3() {
        final MagikCheck check = new FormattingCheck();
        final String code = "a *b";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testAugmentedAssignment1() {
        final MagikCheck check = new FormattingCheck();
        final String code = "a *<< b";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testAugmentedAssignment2() {
        final MagikCheck check = new FormattingCheck();
        final String code = "a _orif<< b";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testBracketProper1() {
        final MagikCheck check = new FormattingCheck();
        final String code = "show(a, b)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testBracketProper2() {
        final MagikCheck check = new FormattingCheck();
        final String code = "show(% )";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testBracketProper3() {
        final MagikCheck check = new FormattingCheck();
        final String code = ""
            + "\t{\n"
            + "\t\t2\n"
            + "\t}\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testBracketProper4() {
        final MagikCheck check = new FormattingCheck();
        final String code = ""
            + "{\r\n"
            + "\t2\r\n"
            + "}\r\n";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testBracketImproper1() {
        final MagikCheck check = new FormattingCheck();
        final String code = "show(a, b )";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testBracketImproper2() {
        final MagikCheck check = new FormattingCheck();
        final String code = "show( a, b)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testSpaces() {
        final MagikCheck check = new FormattingCheck();
        final String code = ".uri         << items[2]";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testPragma() {
        final MagikCheck check = new FormattingCheck();
        final String code = "_pragma(classify_level=restricted, topic={a,b})";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testSyntaxError() {
        final MagikCheck check = new FormattingCheck();
        final String code = "_method";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testFile() throws IllegalArgumentException, IOException {
        final MagikCheck check = new FormattingCheck();
        final Path path = Path.of("magik-checks/src/test/resources/test_product/test_module/source/in_load_list.magik");
        final List<MagikIssue> issues = this.runCheck(path, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testTabIndentLineStartWithTabs() {
        final FormattingCheck check = new FormattingCheck();
        check.indentCharacter = "tab";
        final String code = "\tprint(a)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testTabIndentLineStartWithSpaces() {
        final FormattingCheck check = new FormattingCheck();
        check.indentCharacter = "tab";
        final String code = "                print(a)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void testTabIndentLineStartWithTabsSpaces() {
        final FormattingCheck check = new FormattingCheck();
        check.indentCharacter = "tab";
        final String code = " \tprint(a)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

    @Test
    void tetSpaceIndentLineStartWithTabs() {
        final FormattingCheck check = new FormattingCheck();
        check.indentCharacter = "space";
        final String code = "\tprint(a)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();

    }

    @Test
    void testSpaceIndentLineStrtWithSpaces() {
        final FormattingCheck check = new FormattingCheck();
        check.indentCharacter = "space";
        final String code = "                print(a)";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isEmpty();
    }

    @Test
    void testEmptyLineAfterTransmit() {
        final MagikCheck check = new FormattingCheck();
        final String code = "$\n$";
        final List<MagikIssue> issues = this.runCheck(code, check);
        assertThat(issues).isNotEmpty();
    }

}
