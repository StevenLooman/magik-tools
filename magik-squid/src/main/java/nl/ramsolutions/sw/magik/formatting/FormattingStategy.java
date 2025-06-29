package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;

/** Abstract formatting strategy. */
abstract class FormattingStategy {

  @SuppressWarnings("checkstyle:VisibilityModifier")
  protected Token lastToken;

  @SuppressWarnings("checkstyle:VisibilityModifier")
  protected Token lastTextToken;

  @SuppressWarnings("checkstyle:VisibilityModifier")
  protected FormattingOptions options;

  /**
   * Constructor.
   *
   * @param options Options.
   */
  FormattingStategy(final FormattingOptions options) {
    this.options = options;
  }

  /**
   * Edit newline before token.
   *
   * @param token Token.
   * @return TextEdit, if any.
   */
  @CheckForNull
  protected TextEdit editNewlineBefore(final Token token) {
    if (!AstQuery.tokenIs(token, GenericTokenType.EOL)) {
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
    if (AstQuery.tokenIs(token, GenericTokenType.EOL)) {
      return new TextEdit(
          new Range(new Position(token.getLine() - 1, 0), new Position(token.getLine(), 0)),
          "",
          "no empty line allowed");
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
    if (this.lastToken == null || !AstQuery.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
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
    if (AstQuery.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
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

  /**
   * Set last token.
   *
   * @param token Token to set.
   */
  protected void setLastToken(final Token token) {
    if (!AstQuery.tokenIs(
        token, GenericTokenType.WHITESPACE, GenericTokenType.EOL, GenericTokenType.EOF)) {
      this.lastTextToken = token;
    }

    this.lastToken = token;
  }
}
