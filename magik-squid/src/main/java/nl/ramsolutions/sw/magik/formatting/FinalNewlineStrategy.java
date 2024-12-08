package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import java.util.Collections;
import java.util.List;
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
  List<TextEdit> walkWhitespaceToken(final Token token) {
    // We're activated after last magik-related token.
    // What is left is:
    // - whitespace
    // - comments
    // - EOLs
    // - EOF
    // Any whitespace can be trimmed.
    if (this.options.isTrimTrailingWhitespace() && this.lastToken != null) {
      final TextEdit textEdit = this.editToken(token, "", "no whitespace after allowed");
      return List.of(textEdit);
    }

    return Collections.emptyList();
  }

  @Override
  List<TextEdit> walkCommentToken(final Token token) {
    if (this.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
      final TextEdit textEdit = this.editToken(this.lastToken, "", "no whitespace after allowed");
      return List.of(textEdit);
    }

    return Collections.emptyList();
  }

  @Override
  List<TextEdit> walkEofToken(final Token token) {
    if (this.options.isInsertFinalNewline()
        && !this.tokenIs(this.lastToken, GenericTokenType.EOL)) {
      final TextEdit textEdit =
          this.insertBeforeToken(
              token, FinalNewlineStrategy.EOL_TOKEN_VALUE, "final newline required");
      return List.of(textEdit);
    } else if (this.options.isTrimFinalNewlines() && !token.isOnSameLineThan(this.lastTextToken)) {
      final TextEdit textEdit = this.trimFinalNewlines(token);
      return List.of(textEdit);
    }

    return Collections.emptyList();
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
    return new TextEdit(range, "", "no final newline allowed");
  }
}
