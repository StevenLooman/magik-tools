package nl.ramsolutions.sw.magik.formatting.next;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;

class FinalNewlineWalker extends FormattingWalker2 {

  FinalNewlineWalker(final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);
  }

  @Override
  protected void walkPostMagik(final AstNode node) {
    final Token lastToken = node.getLastToken();
    if (this.getOptions().isTrimFinalNewlines()) {
      this.trimFinalNewlines(lastToken);
    }

    if (this.getOptions().isInsertFinalNewline()) {
      this.insertFinalNewline(lastToken);
    }
  }

  private void trimFinalNewlines(final Token lastToken) {
    final List<Trivia> lastTokenTrivias = lastToken.getTrivia();
    final List<Token> tokensToRemove = new ArrayList<>();
    for (int i = lastTokenTrivias.size() - 1; i >= 0; --i) {
      final Trivia trivia = lastTokenTrivias.get(i);
      final Token triviaToken = trivia.getToken();
      if (!triviaToken.getType().equals(GenericTokenType.EOL)) {
        break;
      }

      tokensToRemove.add(triviaToken);
    }

    tokensToRemove.forEach(this::removeEolToken);
  }

  private void insertFinalNewline(final Token lastToken) {
    final List<Trivia> lastTokenTrivias = lastToken.getTrivia();
    final Trivia lastTrivia =
        !lastTokenTrivias.isEmpty() ? lastTokenTrivias.get(lastTokenTrivias.size() - 1) : null;

    if (lastTrivia == null || lastTrivia.getToken().getType().equals(GenericTokenType.EOL)) {
      this.ensureEolBefore(lastToken);
    }
  }
}
