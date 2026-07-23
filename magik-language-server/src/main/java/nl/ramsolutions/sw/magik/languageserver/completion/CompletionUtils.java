package nl.ramsolutions.sw.magik.languageserver.completion;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import org.eclipse.lsp4j.Position;

/** Shared helpers for completion modules. */
final class CompletionUtils {

  private CompletionUtils() {}

  /**
   * Test whether {@code position} sits within a comment of {@code node}.
   *
   * @param node Top AST node.
   * @param position LSP position.
   * @return {@code true} if the position is in a comment.
   */
  static boolean inComment(final AstNode node, final Position position) {
    final nl.ramsolutions.sw.magik.Position nativePosition =
        Lsp4jConversion.positionFromLsp4j(position);
    return MagikCommentExtractor.extractComments(node)
        .anyMatch(
            token ->
                nativePosition.getLine() == token.getLine()
                    && nativePosition.getColumn() >= token.getColumn());
  }

  /**
   * Range of the {@code prefixLength} characters before the cursor, to be replaced by a completion
   * so it does not duplicate an already-typed prefix like {@code '@'} or {@code 'sw:'}.
   *
   * @param cursor Cursor position.
   * @param prefixLength Number of already-typed characters to replace.
   * @return Replace range.
   */
  static Range replaceRange(final Position cursor, final int prefixLength) {
    final int startChar = Math.max(0, cursor.getCharacter() - prefixLength);
    return new Range(
        Lsp4jConversion.positionFromLsp4j(new Position(cursor.getLine(), startChar)),
        Lsp4jConversion.positionFromLsp4j(cursor));
  }

  /**
   * Compute the range of the identifier being completed: the run of identifier characters (letters,
   * digits, {@code _}, {@code :}, {@code !}, {@code ?}) ending at the cursor. Replacing this range
   * avoids duplicating a package prefix such as {@code sw:} for a completion like {@code sw:rope}.
   *
   * @param line The original source line the cursor is on.
   * @param cursor Cursor position.
   * @return Replace range covering the typed identifier (may be empty at the cursor).
   */
  static Range identifierReplaceRange(final String line, final Position cursor) {
    final int end = Math.min(cursor.getCharacter(), line.length());
    int start = end;
    while (start > 0 && CompletionUtils.isIdentifierPart(line.charAt(start - 1))) {
      start--;
    }
    return new Range(
        Lsp4jConversion.positionFromLsp4j(new Position(cursor.getLine(), start)),
        Lsp4jConversion.positionFromLsp4j(new Position(cursor.getLine(), end)));
  }

  private static boolean isIdentifierPart(final char chr) {
    return Character.isLetterOrDigit(chr) || chr == '_' || chr == ':' || chr == '!' || chr == '?';
  }
}
