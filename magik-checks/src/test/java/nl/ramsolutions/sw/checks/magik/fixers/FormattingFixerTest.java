package nl.ramsolutions.sw.checks.magik.fixers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import org.junit.jupiter.api.Test;

/** Tests for {@link FormattingFixer}. */
@SuppressWarnings("checkstyle:MagicNumber")
class FormattingFixerTest {
  private List<CodeAction> getCodeActions(final String code, final Range range) {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final FormattingFixer fixer = new FormattingFixer();
    return fixer.provideCodeActions(magikFile, range);
  }

  @Test
  void testAddSpaceAfterComma() {
    final String code = "call(arg1,arg2)";
    final Range range = new Range(new Position(0, 0), new Position(2, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions).hasSize(2);
    final CodeAction codeAction = codeactions.get(0);
    assertThat(codeAction)
        .isEqualTo(
            new CodeAction(
                "Fix formatting",
                new TextEdit(new Range(new Position(1, 10), new Position(1, 10)), " ")));
  }

  @Test
  void testRemoveSpaceBeforeComma() {
    final String code = "call(arg1 , arg2)";
    final Range range = new Range(new Position(0, 0), new Position(2, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions).hasSize(2);
    final CodeAction codeAction = codeactions.get(0);
    assertThat(codeAction)
        .isEqualTo(
            new CodeAction(
                "Fix formatting",
                new TextEdit(new Range(new Position(1, 9), new Position(1, 10)), "")));
  }

  @Test
  void testRemoveSpaceAfterBracket() {
    final String code = "call( arg1, arg2)";
    final Range range = new Range(new Position(0, 0), new Position(2, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions).hasSize(2);
    final CodeAction codeAction = codeactions.get(0);
    assertThat(codeAction)
        .isEqualTo(
            new CodeAction(
                "Fix formatting",
                new TextEdit(new Range(new Position(1, 5), new Position(1, 6)), "")));
  }

  @Test
  void testRemoveSpaceBeforeBracket() {
    final String code = "call(arg1, arg2 )";
    final Range range = new Range(new Position(0, 0), new Position(2, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions).hasSize(2);
    final CodeAction codeAction = codeactions.get(0);
    assertThat(codeAction)
        .isEqualTo(
            new CodeAction(
                "Fix formatting",
                new TextEdit(new Range(new Position(1, 15), new Position(1, 16)), "")));
  }

  @Test
  void testRemoveSpaceAfterMethodInvocation() {
    final String code = "xxx. yyy(arg1, arg2)";
    final Range range = new Range(new Position(0, 0), new Position(2, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions).hasSize(2);
    final CodeAction codeAction = codeactions.get(0);
    assertThat(codeAction)
        .isEqualTo(
            new CodeAction(
                "Fix formatting",
                new TextEdit(new Range(new Position(1, 4), new Position(1, 5)), "")));
  }

  @Test
  void testRemoveSpaceBeforeMethodInvocation() {
    final String code = "xxx .yyy(arg1, arg2)";
    final Range range = new Range(new Position(0, 0), new Position(2, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions).hasSize(2);
    final CodeAction codeAction = codeactions.get(0);
    assertThat(codeAction)
        .isEqualTo(
            new CodeAction(
                "Fix formatting",
                new TextEdit(new Range(new Position(1, 3), new Position(1, 4)), "")));
  }

  @Test
  void testRemoveTrailingWhitespace() {
    final String code = "show(1)  \n";
    final Range range = new Range(new Position(0, 0), new Position(3, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                "Fix formatting",
                new TextEdit(new Range(new Position(1, 7), new Position(2, 0)), "\n")));
  }
}
