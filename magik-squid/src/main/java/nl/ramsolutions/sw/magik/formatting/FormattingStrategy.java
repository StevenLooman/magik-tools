package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;

/** Abstract formatting strategy. */
abstract class FormattingStrategy {

  @SuppressWarnings("checkstyle:VisibilityModifier")
  protected Token lastToken;

  @SuppressWarnings("checkstyle:VisibilityModifier")
  protected Token lastTextToken;

  @SuppressWarnings("checkstyle:VisibilityModifier")
  protected FormattingOptions options;

  FormattingStrategy(final FormattingOptions options) {
    this.options = options;
  }

  List<TextEdit> walkWhitespaceToken(final Token token) {
    return Collections.emptyList();
  }

  List<TextEdit> walkCommentToken(final Token token) {
    return Collections.emptyList();
  }

  List<TextEdit> walkEolToken(final Token token) {
    return Collections.emptyList();
  }

  List<TextEdit> walkEofToken(final Token token) {
    return Collections.emptyList();
  }

  List<TextEdit> walkToken(final Token token) {
    return Collections.emptyList();
  }

  void walkPreNode(final AstNode node) {}

  void walkPostNode(final AstNode node) {}

  /**
   * Set last token.
   *
   * @param token Token to set.
   */
  void setLastToken(final Token token) {
    if (!this.tokenIs(
        token, GenericTokenType.WHITESPACE, GenericTokenType.EOL, GenericTokenType.EOF)) {
      this.lastTextToken = token;
    }

    this.lastToken = token;
  }

  /**
   * Edit newline before token.
   *
   * @param token Token.
   * @return TextEdit, if any.
   */
  @CheckForNull
  protected TextEdit editNewlineBefore(final Token token) {
    if (!this.tokenIs(token, GenericTokenType.EOL)) {
      return this.insertBeforeToken(token, "\n", "empty line before is required");
    }

    return null;
  }

  /**
   * Clear newline before last token.
   *
   * @param token Token, unused.
   * @return TextEdit, if changed.
   */
  @CheckForNull
  protected TextEdit editNoNewline(final Token token) {
    if (this.tokenIs(token, GenericTokenType.EOL)) {
      final TextEdit textEdit =
          new TextEdit(
              new Range(new Position(token.getLine() - 1, 0), new Position(token.getLine(), 0)),
              "",
              "no empty line allowed");
      return textEdit;
    }

    return null;
  }

  /**
   * Edit whitespace before token.
   *
   * @param token Token.
   * @return TextEdit, if any.
   */
  @CheckForNull
  protected TextEdit editWhitespaceBefore(final Token token) {
    // Ensure " " before token.
    if (this.lastToken == null || !this.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
      return this.insertBeforeToken(token, " ", "whitespace before required");
    }

    return null;
  }

  /**
   * Clear whitespace before last token.
   *
   * @param token Token, unused.
   * @return TextEdit, if changed.
   */
  @CheckForNull
  protected TextEdit editNoWhitespaceBefore(final Token token) {
    // Ensure no whitespace before token.
    if (this.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
      return this.editToken(this.lastToken, "", "no whitespace before allowed");
    }

    return null;
  }

  /**
   * Edit text of token.
   *
   * @param token Edit this token.
   * @param text New text.
   * @return TextEdit.
   */
  protected TextEdit editToken(final Token token, final String text, final String reason) {
    final int line = token.getLine();
    final int startColumn = token.getColumn();
    final int endColumn = token.getColumn() + token.getOriginalValue().length();

    final Position startPosition = new Position(line, startColumn);
    final Position endPosition = new Position(line, endColumn);
    final Range range = new Range(startPosition, endPosition);
    return new TextEdit(range, text, reason);
  }

  /**
   * Insert text before token.
   *
   * @param token Insert before this token.
   * @param text Text to insert.
   * @return TextEdit.
   */
  protected TextEdit insertBeforeToken(final Token token, final String text, final String reason) {
    final int line = token.getLine();
    final int startColumn = token.getColumn();

    final Position startPosition = new Position(line, startColumn);
    final Position endPosition = new Position(line, startColumn);
    final Range range = new Range(startPosition, endPosition);
    return new TextEdit(range, text, reason);
  }

  protected boolean tokenIs(final @Nullable Token token, final String... values) {
    if (token == null) {
      return false;
    }

    final Set<String> valuesSet = Set.of(values);
    final String tokenValue = token.getOriginalValue();
    return valuesSet.contains(tokenValue);
  }

  protected boolean tokenIs(final @Nullable Token token, final TokenType... types) {
    if (token == null) {
      return false;
    }

    return Stream.of(types).anyMatch(type -> token.getType() == type);
  }
}
