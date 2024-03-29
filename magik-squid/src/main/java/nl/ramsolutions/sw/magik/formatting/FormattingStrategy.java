package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
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

  @CheckForNull
  TextEdit walkWhitespaceToken(final Token token) {
    return null;
  }

  @CheckForNull
  TextEdit walkCommentToken(final Token token) {
    return null;
  }

  @CheckForNull
  TextEdit walkEolToken(final Token token) {
    return null;
  }

  @CheckForNull
  TextEdit walkEofToken(final Token token) {
    return null;
  }

  @CheckForNull
  TextEdit walkToken(final Token token) {
    return null;
  }

  public void walkPreNode(final AstNode node) {}

  public void walkPostNode(final AstNode node) {}

  /**
   * Set last token.
   *
   * @param token Token to set.
   */
  void setLastToken(final Token token) {
    if (token.getType() != GenericTokenType.WHITESPACE
        && token.getType() != GenericTokenType.EOL
        && token.getType() != GenericTokenType.EOF) {
      this.lastTextToken = token;
    }

    this.lastToken = token;
  }

  /**
   * Edit whitespace before token.
   *
   * @param token Token.
   * @return TextEdit, if any.
   */
  protected TextEdit editWhitespaceBefore(final Token token) {
    // Ensure " " before token.
    TextEdit textEdit = null;
    if (this.lastToken == null || this.lastToken.getType() != GenericTokenType.WHITESPACE) {
      textEdit = this.insertBeforeToken(token, " ");
    }
    return textEdit;
  }

  /**
   * Clear whitespace before last token.
   *
   * @param token Token, unused.
   * @return TextEdit, if changed.
   */
  protected TextEdit editNoWhitespaceBefore(final Token token) {
    // Ensure no whitespace before token.
    TextEdit textEdit = null;
    if (this.lastToken != null && this.lastToken.getType() == GenericTokenType.WHITESPACE) {
      textEdit = this.editToken(this.lastToken, "");
    }
    return textEdit;
  }

  /**
   * Edit text of token.
   *
   * @param token Edit this token.
   * @param text New text.
   * @return TextEdit.
   */
  protected TextEdit editToken(final Token token, final String text) {
    final int line = token.getLine();
    final int startColumn = token.getColumn();
    final int endColumn = token.getColumn() + token.getOriginalValue().length();

    final Position startPosition = new Position(line, startColumn);
    final Position endPosition = new Position(line, endColumn);
    final Range range = new Range(startPosition, endPosition);
    return new TextEdit(range, text);
  }

  /**
   * Insert text before token.
   *
   * @param token Insert before this token.
   * @param text Text to insert.
   * @return TextEdit.
   */
  protected TextEdit insertBeforeToken(final Token token, final String text) {
    final int line = token.getLine();
    final int startColumn = token.getColumn();

    final Position startPosition = new Position(line, startColumn);
    final Position endPosition = new Position(line, startColumn);
    final Range range = new Range(startPosition, endPosition);
    return new TextEdit(range, text);
  }
}
