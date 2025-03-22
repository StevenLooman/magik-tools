package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.stream.Stream;

/** Indenting strategy. */
abstract class FormattingIndentStrategy {

  // TODO: Merge with FormattingStrategy. Move whitespacing from FormattingStrategy to separate
  //       strategy base class.

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
  FormattingIndentStrategy(final FormattingOptions options) {
    this.options = options;
  }

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

  protected boolean tokenIs(final @Nullable Token token, final TokenType... types) {
    if (token == null) {
      return false;
    }

    return Stream.of(types).anyMatch(type -> token.getType() == type);
  }

  /**
   * Get the indent for a token.
   *
   * @param lastToken Last token.
   * @param token Token to indent.
   * @param currentNode The current AstNode.
   * @return Indent string.
   */
  abstract String indentFor(final Token token, final AstNode currentNode);
}
