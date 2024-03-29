package nl.ramsolutions.sw.magik.languageserver;

import java.util.Map;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;
import org.eclipse.lsp4j.FileChangeType;

/** Utility class for converting LSP4J <-> MagikLanguageServer. */
public final class Lsp4jConversion {

  private static final Map<FileChangeType, nl.ramsolutions.sw.magik.FileEvent.FileChangeType>
      FILE_CHANGE_TYPE_MAPPING =
          Map.of(
              FileChangeType.Created, nl.ramsolutions.sw.magik.FileEvent.FileChangeType.CREATED,
              FileChangeType.Changed, nl.ramsolutions.sw.magik.FileEvent.FileChangeType.CHANGED,
              FileChangeType.Deleted, nl.ramsolutions.sw.magik.FileEvent.FileChangeType.DELETED);

  private Lsp4jConversion() {}

  /**
   * Convert a Position from LSP4J.
   *
   * @param position Position to convert.
   * @return Position in {@code magik.analysis}.
   */
  public static Position positionFromLsp4j(final org.eclipse.lsp4j.Position position) {
    return new Position(position.getLine() + 1, position.getCharacter());
  }

  /**
   * Convert a Range from LSP4J.
   *
   * @param range Range to convert.
   * @return Range in {@code magik.analysis}.
   */
  public static Range rangeFromLsp4j(final org.eclipse.lsp4j.Range range) {
    return new Range(
        Lsp4jConversion.positionFromLsp4j(range.getStart()),
        Lsp4jConversion.positionFromLsp4j(range.getEnd()));
  }

  /**
   * Convert a Position to LSP4J.
   *
   * @param position Position to convert.
   * @return Position in LSP4J.
   */
  public static org.eclipse.lsp4j.Position positionToLsp4j(final Position position) {
    return new org.eclipse.lsp4j.Position(position.getLine() - 1, position.getColumn());
  }

  /**
   * Convert a Range to LSP4J.
   *
   * @param range Range to convert.
   * @return Range in LSP4J.
   */
  public static org.eclipse.lsp4j.Range rangeToLsp4j(final Range range) {
    final org.eclipse.lsp4j.Position startPosition =
        Lsp4jConversion.positionToLsp4j(range.getStartPosition());
    final org.eclipse.lsp4j.Position endPosition =
        Lsp4jConversion.positionToLsp4j(range.getEndPosition());
    return new org.eclipse.lsp4j.Range(startPosition, endPosition);
  }

  /**
   * Convert a Location to LSP4J.
   *
   * @param location Location to convert.
   * @return Location in LSP4J.
   */
  public static org.eclipse.lsp4j.Location locationToLsp4j(final Location location) {
    final String uri = location.getUri().toString();
    final Range locationRange = location.getRange();
    final Range range =
        locationRange != null ? locationRange : new Range(new Position(1, 0), new Position(1, 0));
    final org.eclipse.lsp4j.Range rangeLsp4j = Lsp4jConversion.rangeToLsp4j(range);
    return new org.eclipse.lsp4j.Location(uri, rangeLsp4j);
  }

  /**
   * Convert a TextEdit to LSP4J.
   *
   * @param textEdit TextEdit to convert.
   * @return TextEdit in LSP4J.
   */
  public static org.eclipse.lsp4j.TextEdit textEditToLsp4j(final TextEdit textEdit) {
    final Range range = textEdit.getRange();
    final String newText = textEdit.getNewText();
    final org.eclipse.lsp4j.Range rangeLsp4j = Lsp4jConversion.rangeToLsp4j(range);
    return new org.eclipse.lsp4j.TextEdit(rangeLsp4j, newText);
  }

  public static FormattingOptions formattingOptionsFromLsp4j(
      final org.eclipse.lsp4j.FormattingOptions options) {
    return new FormattingOptions(
        options.getTabSize(),
        options.isInsertSpaces(),
        options.isInsertFinalNewline(),
        options.isTrimTrailingWhitespace(),
        options.isTrimFinalNewlines());
  }

  public static nl.ramsolutions.sw.magik.FileEvent.FileChangeType fileChangeTypeFromLsp4j(
      final FileChangeType fileChangeType) {
    return Lsp4jConversion.FILE_CHANGE_TYPE_MAPPING.get(fileChangeType);
  }
}
