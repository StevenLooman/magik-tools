package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FormattingCheckTest extends MagikCheckTestBase {

  @Test
  public void testCommaProper1() {
    MagikCheck check = new FormattingCheck();
    String code = "{1, 2}";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testCommaProper2() {
    MagikCheck check = new FormattingCheck();
    String code = "{1, :|a|, 2}";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testCommaImproper1() {
    MagikCheck check = new FormattingCheck();
    String code = "{1,2}";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testCommaImproper2() {
    MagikCheck check = new FormattingCheck();
    String code = "{1 , 2}";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testCommaImproper3() {
    MagikCheck check = new FormattingCheck();
    String code =
        "{1 ,\n" +
        " 2}";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testBinaryOperatorProper1() {
    MagikCheck check = new FormattingCheck();
    String code = "a * b";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testBinaryOperatorProper2() {
    MagikCheck check = new FormattingCheck();
    String code = "a _isnt b";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testBinaryOperatorProper3() {
    MagikCheck check = new FormattingCheck();
    String code = "a +<< b";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testBinaryOperatorImproper1() {
    MagikCheck check = new FormattingCheck();
    String code = "a*b";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testBinaryOperatorImproper2() {
    MagikCheck check = new FormattingCheck();
    String code = "a* b";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testBinaryOperatorImproper3() {
    MagikCheck check = new FormattingCheck();
    String code = "a *b";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testAugmentedAssignment1() {
    MagikCheck check = new FormattingCheck();
    String code = "a *<< b";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testAugmentedAssignment2() {
    MagikCheck check = new FormattingCheck();
    String code = "a _orif<< b";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testBracketProper1() {
    MagikCheck check = new FormattingCheck();
    String code = "show(a, b)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testBracketProper2() {
    MagikCheck check = new FormattingCheck();
    String code = "show(% )";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testBracketProper3() {
    MagikCheck check = new FormattingCheck();
    String code =
        "\t{\n" +
        "\t\t2\n" +
        "\t}\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testBracketProper4() {
    MagikCheck check = new FormattingCheck();
    String code =
        "{\r\n" +
        "\t2\r\n" +
        "}\r\n";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testBracketImproper1() {
    MagikCheck check = new FormattingCheck();
    String code = "show(a, b )";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testBracketImproper2() {
    MagikCheck check = new FormattingCheck();
    String code = "show( a, b)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testMagik() {
    MagikCheck check = new FormattingCheck();
    String code = ".uri     << items[2]";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testPragma() {
    MagikCheck check = new FormattingCheck();
    String code = "_pragma(classify_level=restricted, topic={a,b})";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testSyntaxError() {
    MagikCheck check = new FormattingCheck();
    String code = "_method";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testFile() throws IllegalArgumentException, IOException {
    MagikCheck check = new FormattingCheck();
    Path path = Paths.get("magik-checks/src/test/resources/test_product/test_module/source/in_load_list.magik");
    List<MagikIssue> issues = runCheck(path, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testLineStartWithTabs() {
    MagikCheck check = new FormattingCheck();
    String code = "\tprint(a)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testLineStartWithSpaces() {
    MagikCheck check = new FormattingCheck();
    String code = "        print(a)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testLineStartWithTabsSpaces() {
    MagikCheck check = new FormattingCheck();
    String code = " \tprint(a)";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

}
