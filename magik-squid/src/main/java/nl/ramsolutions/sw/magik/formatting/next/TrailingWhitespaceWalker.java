package nl.ramsolutions.sw.magik.formatting.next;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;

class TrailingWhitespaceWalker extends FormattingWalker2 {

  TrailingWhitespaceWalker(final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);
  }

  @Override
  protected void walkToken(final Token token) {
    if (!this.getOptions().isTrimTrailingWhitespace()) {
      return;
    }

    final List<Token> tokensToRemove = this.findWhitespaceTokensBeforeEolTokens(token);
    tokensToRemove.forEach(this::removeWhitespaceToken);
  }

  private List<Token> findWhitespaceTokensBeforeEolTokens(final Token token) {
    final List<Token> tokensToRemove = new ArrayList<>();

    // Find Trivia which are whitespace, directly before a EOL trivia/token.
    final List<Trivia> trivias = token.getTrivia();
    for (int i = 0; i < trivias.size(); ++i) {
      final Trivia trivia = trivias.get(i);
      final Token triviaToken = trivia.getToken();
      if (triviaToken.getType().equals(GenericTokenType.COMMENT)) {
        final String comment = triviaToken.getOriginalValue();
        final String trimmedComment = comment.trim();
        if (!comment.equals(trimmedComment)) {
          // Update comment in place.
          this.setTokenOriginalValue(triviaToken, trimmedComment);
        }
      }

      if (!triviaToken.getType().equals(GenericTokenType.WHITESPACE)) {
        continue;
      }

      final Trivia nextTrivia = i < trivias.size() - 1 ? trivias.get(i + 1) : null;
      final Token nextTriviaToken = nextTrivia != null ? nextTrivia.getToken() : null;
      if (nextTriviaToken == null
          || !(nextTriviaToken.getType().equals(GenericTokenType.EOL)
              || nextTriviaToken.getType().equals(GenericTokenType.EOF))) {
        continue;
      }

      tokensToRemove.add(triviaToken);
    }

    return tokensToRemove;
  }
}
