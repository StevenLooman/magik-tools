package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;

/** Final newline strategy. */
class FinalNewlineStrategy extends FormattingStrategy {

  private static final String EOL_TOKEN_VALUE = "\n";

  FinalNewlineStrategy(final FormattingOptions options) {
    super(options);
  }

  @Override
  TextEdit walkWhitespaceToken(final Token token) {
    // We're activated after last magik-related token.
    // What is left is:
    // - whitespace
    // - comments
    // - EOLs
    // - EOFs
    // Any whitespace can be trimmed.
    TextEdit textEdit = null;
    if (this.options.isTrimTrailingWhitespace() && this.lastToken != null) {
      textEdit = this.editToken(token, "");
    }
    return textEdit;
  }

  @Override
  TextEdit walkCommentToken(final Token token) {
    TextEdit textEdit = null;
    if (this.lastToken != null && this.lastToken.getType() == GenericTokenType.WHITESPACE) {
      textEdit = this.editToken(this.lastToken, "");
    }
    return textEdit;
  }

  @Override
  TextEdit walkEofToken(final Token token) {
    if (this.options.isInsertFinalNewline() && this.lastToken.getType() != GenericTokenType.EOL) {
      return this.insertBeforeToken(token, FinalNewlineStrategy.EOL_TOKEN_VALUE);
    } else if (this.options.isTrimFinalNewlines()
        && this.lastTextToken != null
        && this.lastTextToken.getLine() != token.getLine()) {
      return this.trimFinalNewlines(token);
    }
    return null;
  }

  /**
   * Trim final newlines.
   *
   * @param token Token.
   */
  private TextEdit trimFinalNewlines(final Token token) {
    final int startLine = this.lastTextToken.getLine() + 1;
    final int startColumn =
        this.lastTextToken.getColumn() + this.lastTextToken.getOriginalValue().length();
    final Position startPosition = new Position(startLine, startColumn);

    final int endLine = token.getLine() + 1;
    final int endColumn = token.getColumn();
    final Position endPosition = new Position(endLine, endColumn);

    final Range range = new Range(startPosition, endPosition);
    return new TextEdit(range, "");
  }
}
