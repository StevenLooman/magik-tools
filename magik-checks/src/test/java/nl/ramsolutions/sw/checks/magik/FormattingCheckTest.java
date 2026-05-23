package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test {@link FormattingCheck}.
 *
 * <p>These tests use the {@link nl.ramsolutions.sw.magik.formatting.TabbedIndentStrategy} indent
 * strategy.
 */
class FormattingCheckTest {

  private static final String RELATIVE_INDENT_STRATEGY = "tab";

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        {1, 2}
        """,
        """
        {1, :a, 2}
        """,
        """
        a * b
        """,
        """
        a _isnt b
        """,
        """
        a +<< b
        """,
        """
        a *<< b
        """,
        """
        a _orif<< b
        """,
        """
        show(a, b)
        """,
        """
        show(% )
        """,
        """
        .uri << items[2]
        """,
        """
        _pragma(classify_level=restricted, topic={a, b})
        """,
        """
        _method
        """,
        """
        {
        	2
        }
        """,
        """
        {\r
        	3\r
        }\r
        """,
        """
        show( # comment
        	param1)
        """,
      })
  void testProper(final String code) {
    final FormattingCheck check = new FormattingCheck();
    check.indentStrategy = FormattingCheckTest.RELATIVE_INDENT_STRATEGY;
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        {1,2}
        """,
        """
        {1 , 2}
        """,
        """
        {1 ,
        \t2}
        """,
        """
        a* b
        """,
        """
        a *b
        """,
        """
        show(a, b )
        """,
        """
        show( a, b)
        """,
        """
        $
        $
        """,
      })
  void testImproper(final String code) {
    final FormattingCheck check = new FormattingCheck();
    check.indentStrategy = FormattingCheckTest.RELATIVE_INDENT_STRATEGY;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testFile() throws IllegalArgumentException, IOException {
    final MagikCheck check = new FormattingCheck();
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/modules/test_module/source/in_load_list.magik");
    assertThat(check).reportsNoIssues(path);
  }

  @Test
  void testTabIndentLineStartWithTabs() {
    final FormattingCheck check = new FormattingCheck();
    check.indentStrategy = FormattingCheckTest.RELATIVE_INDENT_STRATEGY;
    check.indentCharacter = "tab";
    final String code =
        """
        _block
        	print(a)
        _endblock
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
                        print(a)
        """,
        """
        _block
         \tprint(a)
        _endblock
        """,
      })
  void testTabIndentLineStartWithSpaces(final String code) {
    final FormattingCheck check = new FormattingCheck();
    check.indentStrategy = FormattingCheckTest.RELATIVE_INDENT_STRATEGY;
    check.indentCharacter = "tab";
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testSpaceIndentLineStartWithTabs() {
    final FormattingCheck check = new FormattingCheck();
    check.indentStrategy = FormattingCheckTest.RELATIVE_INDENT_STRATEGY;
    check.indentCharacter = "space";
    final String code =
        """
        \tprint(a)
        """;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testSpaceIndentLineStrtWithSpaces() {
    final FormattingCheck check = new FormattingCheck();
    check.indentStrategy = FormattingCheckTest.RELATIVE_INDENT_STRATEGY;
    check.indentCharacter = "space";
    check.tabWidth = 4;
    final String code =
        """
        _block
            print(a)
        _endblock
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _package user


        def_slotted_exemplar(:a, {})
        $
        """,
        """
        $


        _pragma(classify_level=basic)
        _method a.a(parameter)
        _endmethod
        """,
      })
  @SuppressWarnings("java:S4144")
  void testMultipleWhitelines(final String code) {
    final FormattingCheck check = new FormattingCheck();
    check.indentStrategy = FormattingCheckTest.RELATIVE_INDENT_STRATEGY;
    assertThat(check).reportsIssueCount(code, 1);
  }

  @Test
  void testMultipleWhitelinesMethodDoc() {
    final FormattingCheck check = new FormattingCheck();
    check.indentStrategy = FormattingCheckTest.RELATIVE_INDENT_STRATEGY;
    final String code =
        """
        _method object.method(param)
        	##

        	>> param + 1
        _endmethod
        $
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @Test
  void testUnknownIndentStrategyReportsIssue() {
    final FormattingCheck check = new FormattingCheck();
    check.indentStrategy = "visua";
    final String code =
        """
        a << b
        """;
    // A misconfigured indent strategy is reported as a single issue instead of throwing.
    assertThat(check).reportsIssueCount(code, 1);
  }
}
