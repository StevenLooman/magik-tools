package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;

/** Tracks the new positions of tokens, after a {@link TextEdit} would be applied. */
class TokenColumnTracker {

  final Map<Integer, List<Token>> lineTokens;
  final Map<Token, Integer> tokenNewColumns = new HashMap<>();

  /**
   * Constructor.
   *
   * @param magikNode The node to track.
   */
  TokenColumnTracker(final AstNode magikNode) {
    this.lineTokens =
        magikNode.getTokens().stream()
            .collect(
                Collectors.groupingBy(
                    Token::getLine, Collectors.mapping(token -> token, Collectors.toList())));
  }

  /**
   * Get the new column of a token.
   *
   * @param token The token to get the new column for.
   * @return The new column of the token.
   */
  int getNewColumn(final Token token) {
    return this.tokenNewColumns.getOrDefault(token, token.getColumn());
  }

  /**
   * Apply a {@link TextEdit} to the tracked tokens.
   *
   * @param textEdit The {@link TextEdit} to apply.
   */
  void applyTextEdit(final TextEdit textEdit) {
    final int startLine = textEdit.getRange().getStartPosition().getLine();
    final int endLine = textEdit.getRange().getEndPosition().getLine();
    if (startLine != endLine) {
      // Don't try to track across lines.
      return;
    }

    // Determine the change of the TextEdit.
    final Range range = textEdit.getRange();
    final int startColumn = range.getStartPosition().getColumn();
    final int endColumn = range.getEndPosition().getColumn();
    final int originalLength = endColumn - startColumn;
    final int newLength = textEdit.getNewText().length();
    final int offset = newLength - originalLength;

    // Update the new columns of the tokens in the range.
    for (int line = startLine; line <= endLine; line++) {
      final List<Token> tokens = this.lineTokens.getOrDefault(line, Collections.emptyList());
      for (final Token token : tokens) {
        final int newColumn = this.getNewColumn(token) + offset;
        this.tokenNewColumns.put(token, newColumn);
      }
    }
  }
}
