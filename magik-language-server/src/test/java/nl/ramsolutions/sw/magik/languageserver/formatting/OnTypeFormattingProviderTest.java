package nl.ramsolutions.sw.magik.languageserver.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/** Test OnTypeFormattingProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class OnTypeFormattingProviderTest {

  private List<TextEdit> getOnTypeEdits(
      final String code, final Position position, final String triggerChar) {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final FormattingOptions options = new FormattingOptions();
    final FormattingProvider formattingProvider = new FormattingProvider();
    final OnTypeFormattingProvider provider = new OnTypeFormattingProvider(formattingProvider);
    return provider.provideOnTypeFormatting(magikFile, options, position, triggerChar);
  }

  private List<TextEdit> getOnTypeEdits(
      final String code,
      final Position position,
      final String triggerChar,
      final FormattingOptions options) {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final FormattingProvider formattingProvider = new FormattingProvider();
    final OnTypeFormattingProvider provider = new OnTypeFormattingProvider(formattingProvider);
    return provider.provideOnTypeFormatting(magikFile, options, position, triggerChar);
  }

  @Test
  void testNewlineTriggerAlreadyFormatted() {
    // Code is already well-formatted; pressing Enter at end of line 0 should produce no edits.
    final String code =
        """
        _method a.b()
        _endmethod
        """;
    // Position is on the newly-created blank line (line 1).
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(1, 0), "\n");
    assertThat(edits).isEmpty();
  }

  @Test
  void testNewlineTriggerFixesPreviousLine() {
    // The method line has extra space before the dot — formatter should fix it.
    final String code =
        """
        _method a. b()
        _endmethod
        """;
    // Position is on line 1 (new line); the edit touches line 0.
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(1, 0), "\n");
    assertThat(edits).isNotEmpty();
  }

  @Test
  void testStatementSeparatorTriggerCurrentLineOnly() {
    // The method line has extra space; $ is typed on line 0.
    final String code =
        """
        _method a. b()
        _endmethod
        """;
    // Position is on line 0; trigger is $, so only line 0 edits should come back.
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(0, 14), "$");
    // All returned edits must be on line 0.
    assertThat(edits)
        .allSatisfy(edit -> assertThat(edit.getRange().getStart().getLine()).isEqualTo(0));
  }

  @Test
  void testStatementSeparatorNoEditsOnSyntaxError() {
    // Syntax error prevents $ formatting.
    final String code = "_method a. b(\n";
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(0, 0), "$");
    assertThat(edits).isEmpty();
  }

  @Test
  void testNewlineTriggerOnFirstLine() {
    // Edge case: newline trigger when position.line == 0 — must not throw.
    final String code =
        """
        _method a.b()
        _endmethod
        """;
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(0, 0), "\n");
    assertThat(edits).isNotNull();
  }

  @Test
  void testUnknownTriggerReturnsEmpty() {
    final String code =
        """
        _method a.b()
        _endmethod
        """;
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(1, 0), "}");
    assertThat(edits).isEmpty();
  }

  // region: keyword indent heuristic tests

  private static FormattingOptions tabOptions() {
    return new FormattingOptions(8, false);
  }

  private static FormattingOptions spaceOptions(final int tabSize) {
    return new FormattingOptions(tabSize, true);
  }

  @Test
  void testIndentAfterThen() {
    // After typing "_then\n", the new line should be indented by one tab.
    // File has a syntax error (_endif is missing) — heuristic must still work.
    final String code = "_if a\n_then\n";
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(2, 0), "\n", tabOptions());
    assertThat(edits).hasSize(1);
    assertThat(edits.get(0).getNewText()).isEqualTo("\t");
    assertThat(edits.get(0).getRange().getStart().getLine()).isEqualTo(2);
  }

  @Test
  void testIndentAfterLoop() {
    final String code = "_loop\n";
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(1, 0), "\n", tabOptions());
    assertThat(edits).hasSize(1);
    assertThat(edits.get(0).getNewText()).isEqualTo("\t");
  }

  @Test
  void testIndentAfterLoopWithSpaces() {
    final String code = "_loop\n";
    final List<TextEdit> edits =
        this.getOnTypeEdits(code, new Position(1, 0), "\n", spaceOptions(4));
    assertThat(edits).hasSize(1);
    assertThat(edits.get(0).getNewText()).isEqualTo("    ");
  }

  @Test
  void testIndentAfterBlock() {
    final String code = "_block\n";
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(1, 0), "\n", tabOptions());
    assertThat(edits).hasSize(1);
    assertThat(edits.get(0).getNewText()).isEqualTo("\t");
  }

  @Test
  void testNoExtraIndentAfterEndloop() {
    // After _endloop, the new line should stay at the _endloop's indent level (none here).
    final String code = "\t_loop\n\t\tdo_something()\n_endloop\n";
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(3, 0), "\n", tabOptions());
    // _endloop has no leading whitespace — next line should also have none (empty string).
    // That means no insert edit at all (empty string insert is a no-op).
    assertThat(edits).allSatisfy(edit -> assertThat(edit.getNewText()).isEmpty());
  }

  @Test
  void testIndentPreservesExistingLevel() {
    // Inside an already-indented block, a plain statement should continue at same indent.
    final String code = "\t_loop\n\t\tdo_something()\n";
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(2, 0), "\n", tabOptions());
    // Previous line "\t\tdo_something()" has indent "\t\t"; no opener/closer keyword.
    assertThat(edits).hasSize(1);
    assertThat(edits.get(0).getNewText()).isEqualTo("\t\t");
  }

  @Test
  void testNoEditsWhenSyntaxErrorAndNoOpener() {
    // With a syntax error and no opener keyword, the heuristic still produces an indent edit
    // matching the previous line's indent (here: none for a bare "x << 1" at column 0).
    final String code = "x << 1\n";
    final List<TextEdit> edits = this.getOnTypeEdits(code, new Position(1, 0), "\n", tabOptions());
    // No opener — indent is empty, so no insert edit expected.
    assertThat(edits).allSatisfy(edit -> assertThat(edit.getNewText()).isEmpty());
  }

  // endregion
}
