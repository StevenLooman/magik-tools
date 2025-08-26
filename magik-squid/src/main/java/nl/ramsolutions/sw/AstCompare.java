package nl.ramsolutions.sw;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** {@link AstNode} compare tools. */
public final class AstCompare {

  /**
   * Flags to influence the compare functionality.
   *
   * <p>Values:
   *
   * <ul>
   *   <li>{@code COMPARE_LOCATION}: Compare the location of the nodes.
   *   <li>{@code COMPARE_TRIVIA}: Compare the trivia of the nodes.
   * </ul>
   */
  public enum Flags {
    COMPARE_LOCATION,
    COMPARE_TRIVIA,
  }

  private AstCompare() {}

  /**
   * Test if two nodes are equal to each other, recursively.
   *
   * @param left Node to compare
   * @param right Node to compare
   * @param flags Flags to influence comparison
   * @return True if nodes are equal, false otherwise.
   */
  public static boolean astNodeEqualsRecursive(
      final AstNode left, final AstNode right, final Flags... flags) {
    // Compare nodes.
    if (!AstCompare.astNodeEquals(left, right, flags)) {
      return false;
    }

    // Compare children of nodes.
    final List<AstNode> leftChildren = left.getChildren();
    final List<AstNode> rightChildren = right.getChildren();
    if (leftChildren.size() != rightChildren.size()) {
      return false;
    }

    for (int i = 0; i < leftChildren.size(); ++i) {
      final AstNode leftChild = leftChildren.get(i);
      final AstNode rightChild = rightChildren.get(i);
      if (!AstCompare.astNodeEqualsRecursive(leftChild, rightChild, flags)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Test if two nodes are equal to each other.
   *
   * @param left Node to compare
   * @param right Node to compare
   * @param flags Flags to influence comparison
   * @return True if nodes are equal, false otherwise.
   */
  @SuppressWarnings("checkstyle:EmptyBlock")
  public static boolean astNodeEquals(
      final AstNode left, final AstNode right, final Flags... flags) {
    // Ensure same type.
    if (left.getType() != right.getType()) {
      return false;
    }

    final Token leftToken = left.getToken();
    final Token rightToken = right.getToken();
    if (leftToken == null && rightToken == null) {
      // Both tokens are null, so they are equal.
      return true;
    }

    if (leftToken == null || rightToken == null) {
      // One of the tokens is null, so they are not equal.
      return false;
    }

    return AstCompare.tokenEquals(leftToken, rightToken, flags);
  }

  /**
   * Test if two tokens are equal to each other.
   *
   * @param left Token to compare.
   * @param right Token to compare.
   * @param flags Flags to influence comparison.
   * @return True if tokens are equal, false otherwise.
   */
  public static boolean tokenEquals(final Token left, final Token right, final Flags... flags) {
    // Ensure same type.
    if (left.getType() != right.getType()) {
      return false;
    }

    // Compare token values.
    final String leftValue = left.getOriginalValue();
    final String rightValue = right.getOriginalValue();
    if (!Objects.equals(leftValue, rightValue)) {
      return false;
    }

    // Compare locations.
    if (Arrays.asList(flags).contains(Flags.COMPARE_LOCATION)) {
      if (left.getLine() != right.getLine()) {
        return false;
      }

      if (left.getColumn() != right.getColumn()) {
        return false;
      }
    }

    // Compare trivia.
    if (Arrays.asList(flags).contains(Flags.COMPARE_TRIVIA)) {
      final List<Trivia> leftTrivia = left.getTrivia();
      final List<Trivia> rightTrivia = right.getTrivia();

      if (leftTrivia.size() != rightTrivia.size()) {
        return false;
      }

      for (int i = 0; i < leftTrivia.size(); ++i) {
        final Trivia leftTriviaItem = leftTrivia.get(i);
        final Trivia rightTriviaItem = rightTrivia.get(i);
        if (!AstCompare.triviaEquals(leftTriviaItem, rightTriviaItem, flags)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Test if two {@link Trivia}s are equal to each other.
   *
   * @param left Left {@link Trivia} to compare.
   * @param right Right {@link Trivia} to compare.
   * @param flags Flags to influence comparison.
   * @return True if trivia are equal, false otherwise.
   */
  public static boolean triviaEquals(final Trivia left, final Trivia right, final Flags... flags) {
    // Ensure same type.
    if (left.isComment() != right.isComment()) {
      return false;
    }

    if (left.isPreprocessor() != right.isPreprocessor()) {
      return false;
    }

    if (left.isSkippedText() != right.isSkippedText()) {
      return false;
    }

    // Compare trivia tokens.
    final List<Token> leftTokens = left.getTokens();
    final List<Token> rightTokens = right.getTokens();
    if (leftTokens.size() != rightTokens.size()) {
      return false;
    }

    for (int i = 0; i < leftTokens.size(); ++i) {
      final Token leftToken = leftTokens.get(i);
      final Token rightToken = rightTokens.get(i);
      if (!AstCompare.tokenEquals(leftToken, rightToken, flags)) {
        return false;
      }
    }

    return true;
  }
}
