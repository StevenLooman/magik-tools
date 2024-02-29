package nl.ramsolutions.sw.magik.checks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test FormattingCheck. */
class FormattingCheckTest extends MagikCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{1, 2}",
        "{1, :|a|, 2}",
        "a * b",
        "a _isnt b",
        "a +<< b",
        "a *<< b",
        "a _orif<< b",
        "show(a, b)",
        "show(% )",
        ".uri         << items[2]",
        "_pragma(classify_level=restricted, topic={a,b})",
        "_method",
        "" + "\t{\n" + "\t\t2\n" + "\t}\n",
        "" + "{\r\n" + "\t2\r\n" + "}\r\n",
        "" + "show(  # comment\n" + "  param1)",
      })
  void testProper(final String code) {
    final MagikCheck check = new FormattingCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{1,2}",
        "{1 , 2}",
        "{1 ,\n 2}",
        "a* b",
        "a *b",
        "show(a, b )",
        "show( a, b)",
        "$\n$",
      })
  void testImproper(final String code) {
    final MagikCheck check = new FormattingCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testFile() throws IllegalArgumentException, IOException {
    final MagikCheck check = new FormattingCheck();
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/test_module/source/in_load_list.magik");
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

  @ParameterizedTest
  @ValueSource(
      strings = {
        "                print(a)",
        " \tprint(a)",
      })
  void testTabIndentLineStartWithSpaces(final String code) {
    final FormattingCheck check = new FormattingCheck();
    check.indentCharacter = "tab";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void tetSpaceIndentLineStartWithTabs() {
    final FormattingCheck check = new FormattingCheck();
    check.indentCharacter = "space";
    final String code = "\tprint(a)";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testSpaceIndentLineStrtWithSpaces() {
    final FormattingCheck check = new FormattingCheck();
    check.indentCharacter = "space";
    final String code = "                print(a)";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "" + "_package user\n" + "\n" + "\n" + "def_slotted_exemplar(:a, {})\n" + "$\n",
        ""
            + "$\n"
            + "\n"
            + "\n"
            + "\n"
            + "_pragma(classify_level=basic)\n"
            + "_method a.a(parameter)\n"
            + "_endmethod\n",
      })
  void testMultipleWhitelines(final String code) {
    final MagikCheck check = new FormattingCheck();
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMultipleWhitelinesMethodDoc() {
    final MagikCheck check = new FormattingCheck();
    final String code =
        ""
            + "_method object.method(param)\n"
            + "\t##\n"
            + "\n"
            + "\t>> param + 1\n"
            + "_endmethod\n"
            + "$\n";
    final List<MagikIssue> issues = this.runCheck(code, check);
    assertThat(issues).isEmpty();
  }
}
