package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

class MinMaxNewlinesWalker extends FormattingWalker {

  private static final int MAX_CONSECUTIVE_NEWLINES = 2;
  private Token lastTextToken = null;

  /**
   * Constructor.
   *
   * @param options Formatting options.
   */
  public MinMaxNewlinesWalker(
      final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);
  }

  @Override
  public void walkToken(final Token token) {
    this.removeConsecutiveNewlines(token);
    this.addConsecutiveNewlines(token);
  }

  private void removeConsecutiveNewlines(final Token token) {
    final List<Trivia> trivias = token.getTrivia();

    // Max consecutive EOL handling.
    final List<Token> tokensToRemove = new ArrayList<>();
    int consecutiveNewlines = 0;
    for (int i = 0; i < trivias.size(); ++i) {
      final Trivia trivia = trivias.get(i);
      final Token triviaToken = trivia.getToken();

      if (triviaToken.getType().equals(GenericTokenType.EOL)) {
        consecutiveNewlines++;
      } else {
        // Reset on non-newline trivia.
        consecutiveNewlines = 0;
      }

      if (consecutiveNewlines > MAX_CONSECUTIVE_NEWLINES) {
        tokensToRemove.add(triviaToken);
      }
    }

    tokensToRemove.forEach(this::removeEolToken);
  }

  private void addConsecutiveNewlines(final Token token) {
    final List<Trivia> trivias = token.getTrivia();

    // Min consecutive EOL handling.
    if (AstQuery.tokenIs(this.lastTextToken, MagikPunctuator.DOLLAR.getValue())
        && !token.getType().equals(GenericTokenType.EOF)) {
      // Ensure at least two consecutive EOLs after a dollar token.
      final int triviasSize = trivias.size();
      final Token triviaToken1 = triviasSize > 0 ? trivias.get(triviasSize - 1).getToken() : null;
      final Token triviaToken2 = triviasSize > 1 ? trivias.get(triviasSize - 2).getToken() : null;
      if (this.isNotEolToken(triviaToken1)) {
        this.addEolBefore(token);
      }

      if (this.isNotEolToken(triviaToken2)) {
        this.addEolBefore(token);
      }
    }

    if (!token.getType().equals(GenericTokenType.EOL)
        && !token.getType().equals(GenericTokenType.WHITESPACE)) {
      this.lastTextToken = token;
    }
  }

  private boolean isNotEolToken(final Token triviaToken1) {
    return triviaToken1 == null || !triviaToken1.getType().equals(GenericTokenType.EOL);
  }
}
