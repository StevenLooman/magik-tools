package nl.ramsolutions.sw.checks.magik.fixers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import org.junit.jupiter.api.Test;

/** Tests for {@link UseValueCompareFixer}. */
@SuppressWarnings("checkstyle:MagicNumber")
class UseValueCompareFixerTest {
  private static final String REPLACE_IS_WITH_EQUAL_SIGN =
      "Replace `_is` operator with `=` operator";
  private static final String REPLACE_ISNT_WITH_NOT_EQUAL_SIGN =
      "Replace `_isnt` operator with `<>` operator";

  private List<CodeAction> getCodeActions(final String code, final Range range) {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final UseValueCompareFixer fixer = new UseValueCompareFixer();
    return fixer.provideCodeActions(magikFile, range);
  }

  @Test
  void testReplaceIsWithEquals1() {
    final String code = "\"a\" _is b";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                REPLACE_IS_WITH_EQUAL_SIGN,
                new TextEdit(new Range(new Position(1, 4), new Position(1, 7)), "=")));
  }

  @Test
  void testReplaceIsWithEquals2() {
    final String code = "a _is \"b\"";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                REPLACE_IS_WITH_EQUAL_SIGN,
                new TextEdit(new Range(new Position(1, 2), new Position(1, 5)), "=")));
  }

  @Test
  void testReplaceIsWithEquals3() {
    final String code = "a _is 0.0";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                REPLACE_IS_WITH_EQUAL_SIGN,
                new TextEdit(new Range(new Position(1, 2), new Position(1, 5)), "=")));
  }

  @Test
  void testReplaceIsWithEquals4() {
    final String code = "536870913 _is a";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                REPLACE_IS_WITH_EQUAL_SIGN,
                new TextEdit(new Range(new Position(1, 10), new Position(1, 13)), "=")));
  }

  @Test
  void testReplaceIsWithEquals5() {
    final String code = "a _is 16rffffffffffff";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                REPLACE_IS_WITH_EQUAL_SIGN,
                new TextEdit(new Range(new Position(1, 2), new Position(1, 5)), "=")));
  }

  @Test
  void testReplaceIsntWithNotEquals1() {
    final String code = "\"a\" _isnt \"b\"";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                REPLACE_ISNT_WITH_NOT_EQUAL_SIGN,
                new TextEdit(new Range(new Position(1, 4), new Position(1, 9)), "<>")));
  }

  @Test
  void testReplaceIsntWithNotEquals2() {
    final String code = "a _isnt \"b\"";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                REPLACE_ISNT_WITH_NOT_EQUAL_SIGN,
                new TextEdit(new Range(new Position(1, 2), new Position(1, 7)), "<>")));
  }

  @Test
  void testReplaceIsntWithNotEquals3() {
    final String code = "a _isnt 0.0";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                REPLACE_ISNT_WITH_NOT_EQUAL_SIGN,
                new TextEdit(new Range(new Position(1, 2), new Position(1, 7)), "<>")));
  }

  @Test
  void testReplaceIsntWithNotEquals4() {
    final String code = "536870913 _isnt a";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                REPLACE_ISNT_WITH_NOT_EQUAL_SIGN,
                new TextEdit(new Range(new Position(1, 10), new Position(1, 15)), "<>")));
  }

  @Test
  void testReplaceIsntWithNotEquals5() {
    final String code = "a _isnt 16rffffffffffff";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                REPLACE_ISNT_WITH_NOT_EQUAL_SIGN,
                new TextEdit(new Range(new Position(1, 2), new Position(1, 7)), "<>")));
  }
}
