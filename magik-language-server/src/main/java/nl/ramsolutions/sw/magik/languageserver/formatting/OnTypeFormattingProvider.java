package nl.ramsolutions.sw.magik.languageserver.formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.DocumentOnTypeFormattingOptions;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextEdit;

/** On-type formatting provider. */
public class OnTypeFormattingProvider {

  private static final String TRIGGER_NEWLINE = "\n";
  private static final String TRIGGER_STATEMENT_SEPARATOR = "$";

  private final FormattingProvider formattingProvider;

  public OnTypeFormattingProvider(final FormattingProvider formattingProvider) {
    this.formattingProvider = formattingProvider;
  }

  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setDocumentOnTypeFormattingProvider(
        new DocumentOnTypeFormattingOptions(TRIGGER_NEWLINE, List.of(TRIGGER_STATEMENT_SEPARATOR)));
  }

  /**
   * Provide on-type formatting edits.
   *
   * @param magikFile Magik file.
   * @param options Formatting options.
   * @param position Position after the trigger character was typed.
   * @param triggerChar The character that triggered formatting.
   * @return {@link TextEdit}s.
   */
  public List<TextEdit> provideOnTypeFormatting(
      final MagikFile magikFile,
      final FormattingOptions options,
      final Position position,
      final String triggerChar) {
    if (TRIGGER_NEWLINE.equals(triggerChar)) {
      return this.handleNewline(magikFile, options, position);
    }

    if (TRIGGER_STATEMENT_SEPARATOR.equals(triggerChar)) {
      return this.handleStatementSeparator(magikFile, options, position);
    }

    return Collections.emptyList();
  }

  // region: trigger handlers

  private List<TextEdit> handleNewline(
      final MagikFile magikFile, final FormattingOptions options, final Position position) {
    final int newLine = position.getLine();
    final int previousLineIdx = newLine - 1;
    if (previousLineIdx < 0) {
      return Collections.emptyList();
    }

    final String[] sourceLines = magikFile.getSourceLines();
    if (previousLineIdx >= sourceLines.length) {
      return Collections.emptyList();
    }

    final List<TextEdit> edits = new ArrayList<>();

    // 1. If the AST is valid, also fix spacing on the previous line.
    if (this.formattingProvider.canFormat(magikFile)) {
      final Range prevLineRange = this.lineRange(previousLineIdx);
      final List<TextEdit> prevLineEdits =
          this.formattingProvider.provideRangeFormatting(magikFile, options, prevLineRange);
      edits.addAll(prevLineEdits);
    }

    // 2. Always apply the keyword-based indent heuristic for the new blank line.
    final String previousLine = sourceLines[previousLineIdx];
    final String currentIndent = NewlineIndentHelper.leadingWhitespace(previousLine);
    final boolean useTabs = !options.isInsertSpaces();
    final int tabSize = options.getTabSize();
    final NewlineIndentHelper helper = new NewlineIndentHelper(useTabs, tabSize);
    final String newIndent = helper.computeNewLineIndent(previousLine, currentIndent);

    if (newIndent != null && !newIndent.isEmpty()) {
      // Insert the indentation at the start of the new blank line.
      final org.eclipse.lsp4j.Range insertRange =
          new org.eclipse.lsp4j.Range(new Position(newLine, 0), new Position(newLine, 0));
      edits.add(new TextEdit(insertRange, newIndent));
    }

    return edits;
  }

  private List<TextEdit> handleStatementSeparator(
      final MagikFile magikFile, final FormattingOptions options, final Position position) {
    if (!this.formattingProvider.canFormat(magikFile)) {
      return Collections.emptyList();
    }

    final int line = position.getLine();
    final Range range = this.lineRange(line);
    return this.formattingProvider.provideRangeFormatting(magikFile, options, range);
  }

  // endregion

  private Range lineRange(final int line) {
    final org.eclipse.lsp4j.Range lsp4jRange =
        new org.eclipse.lsp4j.Range(new Position(line, 0), new Position(line, Integer.MAX_VALUE));
    return Lsp4jConversion.rangeFromLsp4j(lsp4jRange);
  }
}
