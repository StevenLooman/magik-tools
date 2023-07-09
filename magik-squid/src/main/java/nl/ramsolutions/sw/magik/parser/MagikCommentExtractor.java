package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.utils.StreamUtils;

/**
 * Comment extractor for Magik sources.
 * The MagikParser, or rather sslr, does not properly store Trivia/comments.
 */
public final class MagikCommentExtractor {

    private MagikCommentExtractor() {
    }

    /**
     * Extract comments from {@link node}.
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
     * @param node Node containing {@link Trivia} with comments.
     * @return Stream of comment {@link Token}s.
     */
    public static Stream<Token> extractLineComments(final AstNode node) {
        return node.getTokens().stream()
            .filter(Token::hasTrivia)
            .flatMap(token -> {
                final Stream<Trivia> streamA = token.getTrivia().stream();
                final Stream<Trivia> streamB = token.getTrivia().stream().skip(1);
                final Stream<List<Token>> streamC = StreamUtils.zip(streamA, streamB)
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .map(entry -> {
                        final Trivia previousTrivia = entry.getKey();
                        final Trivia trivia = entry.getValue();
                        final List<Token> lineCommentTokens = new ArrayList<>();

                        final List<Token> previousTriviaTokens = previousTrivia.getTokens();
                        final Token previousTriviaLastToken = previousTriviaTokens.get(previousTriviaTokens.size() - 1);
                        if (trivia.isComment()
                            && (previousTrivia.isSkippedText()  // Whitespace from start of line.
                                && previousTriviaLastToken.getColumn() == 0
                                || previousTrivia.isSkippedText()  // EOL.
                                && previousTriviaLastToken.getType() == GenericTokenType.EOL)) {
                            final Token lineCommentToken = trivia.getToken();
                            lineCommentTokens.add(lineCommentToken);
                        }

                        return lineCommentTokens;
                    });
                return streamC.flatMap(List::stream);
            });
    }

    /**
     * Extract Doc comments for {@link node}.
     * @param node Node containing {@link Trivia} with comments.
     * @return Stream of Doc comment {@link Token}s.
     */
    public static Stream<Token> extractDocComments(final AstNode node) {
        return node.getTokens().stream()
            .flatMap(token -> token.getTrivia().stream())
            .filter(Trivia::isComment)
            .map(Trivia::getToken)
            .filter(token -> token.getValue().startsWith("##"));
    }

}
