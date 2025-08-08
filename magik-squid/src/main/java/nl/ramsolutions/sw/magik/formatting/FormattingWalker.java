package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.List;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;

/** AST walker for formatting Magik code. */
public abstract class FormattingWalker extends MagikAstWalker {

  private static final String NEWLINE = "\n";

  private final FormattingOptions options;
  private final TokenTriviaEditor tokenEditor;

  /**
   * Constructor.
   *
   * @param options Formatting options.
   */
  FormattingWalker(final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    this.options = options;
    this.tokenEditor = tokenEditor;
  }

  protected FormattingOptions getOptions() {
    return this.options;
  }

  protected void setTokenOriginalValue(final Token token, final String value) {
    this.tokenEditor.setTokenOriginalValue(token, value);
  }

  protected Token getTokenBeforeOnSameLine(final Token token) {
    return this.tokenEditor.getTokenBeforeOnSameLine(token);
  }

  /**
   * Ensures that there is a single whitespace before the given {@link AstNode}, if the token is NOT
   * the first text token of its line.
   *
   * @param node The {@link Token} to get from the {@link AstNode} to check and potentially modify.
   */
  protected void ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(final AstNode node) {
    final Token token = node.getToken();
    if (token == null) {
      throw new IllegalStateException("Node has no token");
    }

    this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(token);
  }

  /**
   * Ensures that there is a single whitespace before the given {@link Token}, if the token is NOT
   * the first text token of its line.
   *
   * @param token The {@link Token} to ensure a whitespace before, if not first text token on line.
   */
  protected void ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(final Token token) {
    // Don't ensure anything when we are the first (text) token on the line.
    if (this.hasNewlineTrivia(token) || this.isFirstTextToken(token)) {
      return;
    }

    this.ensureWhitespaceBefore(token, " ");
  }

  /**
   * Ensures that there is a (single) whitespace before the given {@link Token}.
   *
   * @param token The {@link Token} to check and potentially modify. Can be a regular token or a
   *     trivia token.
   * @param whitespace The whitespace to ensure before the token.
   */
  protected Token ensureWhitespaceBefore(final Token token, final String whitespace) {
    final Token tokenBefore = this.tokenEditor.getTokenBeforeOnSameLine(token);
    if (tokenBefore == null || !tokenBefore.getType().equals(GenericTokenType.WHITESPACE)) {
      // No token before, so we can just add the whitespace.
      return this.tokenEditor.addWhitespaceBefore(token, whitespace);
    }

    if (!tokenBefore.getValue().equals(whitespace)) {
      return this.tokenEditor.setTokenOriginalValue(tokenBefore, whitespace);
    }

    return tokenBefore;
  }

  protected void ensureNoWhitespaceBeforeIfNotFirstTextOnLine(final AstNode node) {
    final Token token = node.getToken();
    if (token == null) {
      throw new IllegalStateException("Node has no token");
    }

    this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(token);
  }

  protected void ensureNoWhitespaceBeforeIfNotFirstTextOnLine(final Token token) {
    // Don't ensure anything when we are the first (text) token on the line.
    if (this.hasNewlineTrivia(token) || this.isFirstTextToken(token)) {
      return;
    }

    this.ensureNoWhitespaceBefore(token);
  }

  /**
   * Ensures that there is no whitespace before the given {@link AstNode}.
   *
   * @param node The {@link Token} to get from the {@link AstNode} to check and potentially modify.
   */
  protected void ensureNoWhitespaceBefore(final AstNode node) {
    final Token token = node.getToken();
    if (token == null) {
      throw new IllegalStateException("Node has no token");
    }

    this.ensureNoWhitespaceBefore(token);
  }

  /**
   * Ensures that there is no whitespace after the given {@link Token}.
   *
   * @param token The {@link Token} to check and potentially modify. Can be a regular token or a
   *     trivia token.
   */
  protected void ensureNoWhitespaceBefore(final Token token) {
    final Token tokenBefore = this.tokenEditor.getTokenBeforeOnSameLine(token);
    if (tokenBefore == null || !tokenBefore.getType().equals(GenericTokenType.WHITESPACE)) {
      // No token before, so we can just add the whitespace.
      return;
    }

    this.tokenEditor.removeWhitespaceToken(tokenBefore);
  }

  protected void removeWhitespaceToken(final Token whitespaceToken) {
    this.tokenEditor.removeWhitespaceToken(whitespaceToken);
  }

  protected void removeEolToken(final Token eolToken) {
    this.tokenEditor.removeEolToken(eolToken);
  }

  protected Token ensureEolBefore(final Token token) {
    final Token beforeToken = this.tokenEditor.getTokenBefore(token);
    if (beforeToken == null || !beforeToken.getType().equals(GenericTokenType.EOL)) {
      // No EOL token before, so we can just add one.
      return this.tokenEditor.addEolBefore(token, NEWLINE);
    }

    return beforeToken;
  }

  protected Token addEolBefore(final Token token) {
    return this.tokenEditor.addEolBefore(token, NEWLINE);
  }

  protected boolean hasNewlineTrivia(final Token token) {
    return token.getTrivia().stream()
        .anyMatch(trivia -> trivia.getToken().getType().equals(GenericTokenType.EOL));
  }

  protected boolean isFirstTextToken(final Token token) {
    final List<Trivia> trivias = token.getTrivia();
    if (trivias.isEmpty()) {
      return token.getLine() == 1 && token.getColumn() == 0;
    }

    final Trivia firstTrivia = trivias.get(0);
    final Token triviaToken = firstTrivia.getToken();
    return triviaToken.getLine() == 1 && triviaToken.getColumn() == 0;
  }
}
