package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.TextEdit;

/** Pragma formatting strategy. */
class PragmaFormattingStrategy extends FormattingStrategy {

  boolean pragmaTokenSeen = false;

  PragmaFormattingStrategy(final FormattingOptions options) {
    super(options);
  }

  @Override
  List<TextEdit> walkWhitespaceToken(final Token token) {
    if (this.pragmaTokenSeen) {
      if (this.tokenIs(this.lastTextToken, ",")) {
        if (!this.tokenIs(token, " ")) {
          // Require whitespace after ",".
          final TextEdit textEdit = this.editToken(token, " ", "whitespace after required");
          return List.of(textEdit);
        }
      } else {
        // Pragma's don't have whitespace otherwise.
        final TextEdit textEdit = this.editToken(token, "", "no whitespace after allowed");
        return List.of(textEdit);
      }
    }

    return Collections.emptyList();
  }

  @Override
  List<TextEdit> walkEolToken(final Token token) {
    if (this.pragmaTokenSeen) {
      // Pragma's don't have newlines.
      final TextEdit textEdit = this.editToken(token, "", "no newline after allowed");
      return List.of(textEdit);
    }

    // `_pragma` not seen yet.
    final int emptyLineCount =
        this.lastTextToken != null ? token.getLine() - this.lastTextToken.getLine() : 0;
    if (emptyLineCount > 1) {
      // Add edit to remove empty line.
      final TextEdit textEdit = this.editNoNewline(token);
      return List.of(textEdit);
    }

    return Collections.emptyList();
  }

  @Override
  List<TextEdit> walkToken(final Token token) {
    if (this.tokenIs(token, "_pragma")) {
      this.pragmaTokenSeen = true;
    }

    if (this.tokenIs(this.lastToken, ",")) {
      final TextEdit textEdit = this.insertBeforeToken(token, " ", "whitespace before required");
      return List.of(textEdit);
    }

    return Collections.emptyList();
  }

  @Override
  void walkPreNode(final AstNode node) {
    this.pragmaTokenSeen = false;
  }
}
