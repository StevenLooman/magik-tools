package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Comment extractor for Magik sources. The MagikParser, or rather sslr, does not properly store
 * Trivia/comments.
 */
public final class MagikCommentExtractor {

  private MagikCommentExtractor() {}

  /**
   * Extract comments from {@link node}.
   *
   * @param node Node containing {@link Trivia} with comments.
   * @return Stream of comment {@link Token}s.
   */
  public static Stream<Token> extractComments(final AstNode node) {
    return node.getTokens().stream()
        .flatMap(token -> token.getTrivia().stream())
        .filter(Trivia::isComment)
        .map(Trivia::getToken);
  }

  /**
   * Extract line comments from {@link node}.
   *
   * @param node Node containing {@link Trivia} with comments.
   * @return Stream of comment {@link Token}s.
   */
  public static Stream<Token> extractLineComments(final AstNode node) {
    return node.getTokens().stream()
        .filter(Token::hasTrivia)
        .flatMap(
            token -> {
              final List<Trivia> trivias = token.getTrivia();
              return trivias.stream()
                  .map(
                      trivia -> {
                        if (!trivia.isComment()) {
                          return null;
                        }

                        final int index = trivias.indexOf(trivia);
                        final Trivia previousTrivia = index > 0 ? trivias.get(index - 1) : null;

                        if (previousTrivia == null) {
                          // First line of source.
                          return trivia.getToken();
                        }

                        if (!previousTrivia.isSkippedText()) {
                          // Regular token? Not a single line comment.
                          return null;
                        }

                        final List<Token> previousTriviaTokens = previousTrivia.getTokens();
                        final Token previousTriviaLastToken =
                            previousTriviaTokens.get(previousTriviaTokens.size() - 1);
                        if (previousTriviaLastToken.getColumn()
                                == 0 // Whitespace from start of line.
                            || previousTriviaLastToken.getType() == GenericTokenType.EOL) {
                          return trivia.getToken();
                        }

                        return null;
                      })
                  .filter(Objects::nonNull);
            });
  }

  /**
   * Extract Doc comment tokens for {@link node}.
   *
   * @param node Node containing {@link Trivia} with comments.
   * @return Stream of Doc comment {@link Token}s.
   */
  public static Stream<Token> extractDocCommentTokens(final AstNode node) {
    return node.getTokens().stream()
        .flatMap(token -> token.getTrivia().stream())
        .filter(Trivia::isComment)
        .map(Trivia::getToken)
        .filter(token -> token.getValue().startsWith("##"));
  }

  /**
   * Extract Doc comment for {@link node}.
   *
   * @param node Node containing {@link Trivia} with comments.
   * @return Doc.
   */
  public static String extractDocComment(final AstNode node) {
    return MagikCommentExtractor.extractDocCommentTokens(node)
        .map(Token::getValue)
        .map(line -> line.substring(2)) // Strip '##'
        .map(String::trim)
        .collect(Collectors.joining("\n"));
  }
}
