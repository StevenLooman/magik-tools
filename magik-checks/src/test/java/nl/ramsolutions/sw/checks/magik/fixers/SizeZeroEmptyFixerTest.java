package nl.ramsolutions.sw.checks.magik.fixers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import org.junit.jupiter.api.Test;

class SizeZeroEmptyFixerTest {
  private List<CodeAction> getCodeActions(final String code, final Range range) {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final SizeZeroEmptyFixer provider = new SizeZeroEmptyFixer();
    return provider.provideCodeActions(magikFile, range);
  }

  @Test
  void testReplaceSizeIsWithEmpty1() {
    final String code = "a.size _is 0";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                "Replace size comparison with `.empty?`",
                new TextEdit(new Range(new Position(1, 0), new Position(1, 12)), "a.empty?")));
  }

  @Test
  void testReplaceSizeIsWithEmpty2() {
    final String code = "a.b.size _is 0";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                "Replace size comparison with `.empty?`",
                new TextEdit(new Range(new Position(1, 0), new Position(1, 14)), "a.b.empty?")));
  }

  @Test
  void testReplaceZeroIsSizeWithEmpty1() {
    final String code = "0 _is a.size";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                "Replace size comparison with `.empty?`",
                new TextEdit(new Range(new Position(1, 0), new Position(1, 12)), "a.empty?")));
  }

  @Test
  void testReplaceZeroIsSizeWithEmpty2() {
    final String code = "0 _is a.b.size";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                "Replace size comparison with `.empty?`",
                new TextEdit(new Range(new Position(1, 0), new Position(1, 14)), "a.b.empty?")));
  }

  @Test
  void testReplaceSizeEqualsZeroWithEmpty1() {
    final String code = "a.size = 0";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                "Replace size comparison with `.empty?`",
                new TextEdit(new Range(new Position(1, 0), new Position(1, 10)), "a.empty?")));
  }

  @Test
  void testReplaceSizeEqualsZeroWithEmpty2() {
    final String code = "a.b.size = 0";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                "Replace size comparison with `.empty?`",
                new TextEdit(new Range(new Position(1, 0), new Position(1, 12)), "a.b.empty?")));
  }

  @Test
  void testReplaceZeroEqualsSizeWithEmpty1() {
    final String code = "0 = a.size";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                "Replace size comparison with `.empty?`",
                new TextEdit(new Range(new Position(1, 0), new Position(1, 10)), "a.empty?")));
  }

  @Test
  void testReplaceZeroEqualsSizeWithEmpty2() {
    final String code = "0 = a.b.size";
    final Range range = new Range(new Position(0, 0), new Position(1, 0));
    final List<CodeAction> codeactions = this.getCodeActions(code, range);
    assertThat(codeactions)
        .containsOnly(
            new CodeAction(
                "Replace size comparison with `.empty?`",
                new TextEdit(new Range(new Position(1, 0), new Position(1, 12)), "a.b.empty?")));
  }
}
