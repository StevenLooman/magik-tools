package nl.ramsolutions.sw;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.List;

/** Helper for {@link Trivia}s. */
public final class TriviaHelper {

  private TriviaHelper() {}

  /**
   * Clone a {@link Trivia} object, with its {@link Token}s.
   *
   * @param trivia The {@link Trivia} to clone.
   * @return A cloned {@link Trivia} object with cloned {@link Token}s.
   */
  public static Trivia clone(final Trivia trivia) {
    if (trivia.isComment()) {
      final Token commentToken = trivia.getToken();
      final Token clonedToken = TokenHelper.clone(commentToken);
      return Trivia.createComment(clonedToken);
    } else if (trivia.isSkippedText()) {
      final List<Token> skippedTokens = trivia.getTokens();
      final List<Token> clonedTokens = skippedTokens.stream().map(TokenHelper::clone).toList();
      return Trivia.createSkippedText(clonedTokens);
    }

    throw new IllegalArgumentException(
        "Cannot clone trivia of type: " + trivia.getClass().getName());
  }
}
