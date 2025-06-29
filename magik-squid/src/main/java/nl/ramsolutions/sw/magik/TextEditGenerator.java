package nl.ramsolutions.sw.magik;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates {@link TextEdit}s based on differences in the AST {@link Token}s.
 *
 * <p>Currently only supports whitepace {@link Trivia} changes in the AST. Should be improved to do
 * a full diff, using for example the Myers Standard Algorithm or similar.
 */
public class TextEditGenerator {

  /**
   * Generate a list of {@link TextEdit}s based on the differences between two {@link AstNode}s.
   *
   * @param nodeA The first {@link AstNode}.
   * @param nodeB The second {@link AstNode}.
   * @return A list of {@link TextEdit}s representing the differences.
   */
  public List<TextEdit> generateEdits(final AstNode nodeA, final AstNode nodeB) {
    final List<TextEdit> edits = new ArrayList<>();

    final List<Token> tokensA = nodeA.getTokens();
    final List<Token> tokensB = nodeB.getTokens();
    if (tokensA.size() != tokensB.size()) {
      throw new IllegalArgumentException("Token counts in AST differ");
    }

    for (int i = 0; i < tokensA.size(); ++i) {
      final Token tokenA = tokensA.get(i);
      final Token tokenB = tokensB.get(i);

      // Compare token types.
      // TODO: Do these ever differ? Only for EOF most likely.
      final TokenType tokenTypeA = tokenA.getType();
      final TokenType tokenTypeB = tokenB.getType();
      if (!tokenTypeA.equals(tokenTypeB)) {
        throw new IllegalArgumentException("Token types in AST differ");
      }

      final List<TextEdit> tokenEdits = this.compareTokenTrivia(tokenA, tokenB);
      edits.addAll(tokenEdits);

      // Compare token original value itself.
      final List<TextEdit> valueEdits = this.compareTokenValues(tokenA, tokenB);
      edits.addAll(valueEdits);
    }

    return edits;
  }

  /**
   * Compare {@link Token} values for differences, and return differences as {@link TextEdit}s.
   *
   * <p>This assumes that the token never crosses a line boundary.
   *
   * @param tokenA The first {@link Token} to compare.
   * @param tokenB The second {@link Token} to compare.
   * @return A list of {@link TextEdit}s representing the differences.
   */
  private List<TextEdit> compareTokenValues(final Token tokenA, final Token tokenB) {
    final String valueA = tokenA.getOriginalValue();
    final String valueB = tokenB.getOriginalValue();
    if (valueA.equals(valueB)) {
      // No differences in values, nothing to do.
      return Collections.emptyList();
    }

    // If the values differ, we assume they are on the same line and column.
    return List.of(
        new TextEdit(
            new Range(
                new Position(tokenA.getLine(), tokenA.getColumn()),
                new Position(tokenA.getLine(), tokenA.getColumn() + valueA.length())),
            valueB));
  }

  /**
   * Compare {@link Token} {@link Trivia} for whitespacing differences, and return differences as
   * {@link TextEdit}s.
   *
   * @param tokenA {@link Token} A to compare.
   * @param tokenB {@link Token} B to compare.
   * @return List of {@link TextEdit}s.
   */
  private List<TextEdit> compareTokenTrivia(final Token tokenA, final Token tokenB) {
    // Assume there is one token per Trivia.
    final List<Trivia> tokenTriviasA = tokenA.getTrivia();
    final List<Trivia> tokenTriviasB = tokenB.getTrivia();

    // For now, we regard all the Trivia as one continuous token.
    // TODO: Replace this with the Myers Standard Algorithm or similar to find
    // differences in the trivia, or perhaps even complete AST.
    final String triviaA = TextEditGenerator.stringifyTrivias(tokenTriviasA);
    final String triviaB = TextEditGenerator.stringifyTrivias(tokenTriviasB);
    if (triviaA.equals(triviaB)) {
      // No differences in trivia, nothing to do.
      return Collections.emptyList();
    }

    final Token startToken = !tokenTriviasA.isEmpty() ? tokenTriviasA.get(0).getToken() : tokenA;
    return List.of(
        new TextEdit(
            new Range(
                new Position(startToken.getLine(), startToken.getColumn()),
                new Position(tokenA.getLine(), tokenA.getColumn())),
            triviaB));
  }

  private static String stringifyTrivias(final List<Trivia> trivias) {
    final StringBuilder stringBuilder = new StringBuilder();

    for (final Trivia trivia : trivias) {
      for (final Token triviaToken : trivia.getTokens()) {
        final String triviaTokenValue = triviaToken.getOriginalValue();
        stringBuilder.append(triviaTokenValue);
      }
    }

    return stringBuilder.toString();
  }
}
