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
 * <p>Currently only supports whitespace {@link Trivia} changes in the AST. Should be improved to do
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
   * <p>Uses the Myers diff algorithm to find minimal differences between trivia, generating precise
   * {@link TextEdit}s that only affect the parts that actually changed.
   *
   * @param tokenA {@link Token} A to compare.
   * @param tokenB {@link Token} B to compare.
   * @return List of {@link TextEdit}s.
   */
  private List<TextEdit> compareTokenTrivia(final Token tokenA, final Token tokenB) {
    // Assume there is one token per Trivia.
    final List<Trivia> tokenTriviasA = tokenA.getTrivia();
    final List<Trivia> tokenTriviasB = tokenB.getTrivia();

    final String triviaA = TextEditGenerator.stringifyTrivias(tokenTriviasA);
    final String triviaB = TextEditGenerator.stringifyTrivias(tokenTriviasB);
    if (triviaA.equals(triviaB)) {
      // No differences in trivia, nothing to do.
      return Collections.emptyList();
    }

    // Use Myers diff algorithm to find minimal differences.
    final MyersDiff differ = new MyersDiff();
    final List<MyersDiff.DiffEdit> diffEdits = differ.diff(triviaA, triviaB);

    // Convert DiffEdits to TextEdits.
    final List<TextEdit> textEdits = new ArrayList<>();

    // Calculate base position - this should be where the trivia starts, not where the main token
    // is.
    final Position basePosition;
    if (!tokenTriviasA.isEmpty() && !tokenTriviasA.get(0).getTokens().isEmpty()) {
      // Use the position of the first token within the trivia.
      final Token firstTriviaToken = tokenTriviasA.get(0).getTokens().get(0);
      basePosition = new Position(firstTriviaToken.getLine(), firstTriviaToken.getColumn());
    } else {
      // No trivia in A, so trivia would start at the main token's position.
      basePosition = new Position(tokenA.getLine(), tokenA.getColumn());
    }

    for (final MyersDiff.DiffEdit diffEdit : diffEdits) {
      if (diffEdit.getType() == MyersDiff.DiffEdit.Type.EQUAL) {
        // Skip equal sections - they don't need edits.
        continue;
      }

      // Calculate the actual position in the document.
      final Position startPos = this.calculatePosition(basePosition, triviaA, diffEdit.getStartA());
      final Position endPos;
      final String newText;

      if (diffEdit.getType() == MyersDiff.DiffEdit.Type.DELETE) {
        // Delete operation: remove text from original.
        endPos =
            this.calculatePosition(
                basePosition, triviaA, diffEdit.getStartA() + diffEdit.getText().length());
        newText = "";
      } else {
        // Insert operation: insert text at position.
        endPos = startPos;
        newText = diffEdit.getText();
      }

      textEdits.add(new TextEdit(new Range(startPos, endPos), newText));
    }

    // Merge consecutive edits for cleaner output.
    return this.mergeConsecutiveEdits(textEdits);
  }

  /**
   * Merge consecutive edits that are adjacent in the document.
   *
   * <p>This combines multiple small edits (like consecutive character insertions or deletions) into
   * larger, more readable edits. Edits are considered consecutive if they are adjacent and of
   * compatible types (both insertions at the same position, or deletions of adjacent ranges).
   *
   * @param edits The list of text edits to merge.
   * @return A new list with consecutive edits merged.
   */
  private List<TextEdit> mergeConsecutiveEdits(final List<TextEdit> edits) {
    if (edits.isEmpty()) {
      return edits;
    }

    final List<TextEdit> merged = new ArrayList<>();
    TextEdit current = edits.get(0);

    for (int i = 1; i < edits.size(); i++) {
      final TextEdit next = edits.get(i);

      // Check if we can merge current with next.
      if (this.canMergeEdits(current, next)) {
        current = this.mergeEdits(current, next);
      } else {
        merged.add(current);
        current = next;
      }
    }

    merged.add(current);
    return merged;
  }

  /**
   * Check if two edits can be merged.
   *
   * @param edit1 First edit.
   * @param edit2 Second edit.
   * @return True if the edits can be merged.
   */
  private boolean canMergeEdits(final TextEdit edit1, final TextEdit edit2) {
    final Range range1 = edit1.getRange();
    final Range range2 = edit2.getRange();

    // Both are insertions at the same position.
    if (range1.getStartPosition().equals(range1.getEndPosition())
        && range2.getStartPosition().equals(range2.getEndPosition())
        && range1.getStartPosition().equals(range2.getStartPosition())) {
      return true;
    }

    // Both are deletions and ranges are adjacent.
    if (edit1.getNewText().isEmpty()
        && edit2.getNewText().isEmpty()
        && range1.getEndPosition().equals(range2.getStartPosition())) {
      return true;
    }

    // First is deletion, second is insertion at the same position.
    if (edit1.getNewText().isEmpty()
        && !edit2.getNewText().isEmpty()
        && range1.getEndPosition().equals(range2.getStartPosition())
        && range2.getStartPosition().equals(range2.getEndPosition())) {
      return true;
    }

    return false;
  }

  /**
   * Merge two compatible edits into one.
   *
   * @param edit1 First edit.
   * @param edit2 Second edit.
   * @return The merged edit.
   */
  private TextEdit mergeEdits(final TextEdit edit1, final TextEdit edit2) {
    final Range range1 = edit1.getRange();
    final Range range2 = edit2.getRange();

    // Both are insertions at the same position - concatenate the text
    if (range1.getStartPosition().equals(range1.getEndPosition())
        && range2.getStartPosition().equals(range2.getEndPosition())
        && range1.getStartPosition().equals(range2.getStartPosition())) {
      return new TextEdit(range1, edit1.getNewText() + edit2.getNewText());
    }

    // Both are deletions - extend the range.
    if (edit1.getNewText().isEmpty() && edit2.getNewText().isEmpty()) {
      return new TextEdit(new Range(range1.getStartPosition(), range2.getEndPosition()), "");
    }

    // Deletion followed by insertion - replace.
    if (edit1.getNewText().isEmpty() && !edit2.getNewText().isEmpty()) {
      return new TextEdit(
          new Range(range1.getStartPosition(), range1.getEndPosition()), edit2.getNewText());
    }

    throw new IllegalStateException("Cannot merge incompatible edits");
  }

  /**
   * Calculate the position in the document given a base position and an offset into a string.
   *
   * <p>This handles newlines in the string, advancing line numbers and resetting column positions
   * appropriately.
   *
   * @param basePosition The starting position.
   * @param text The text containing potential newlines.
   * @param offset The offset into the text.
   * @return The calculated position.
   */
  private Position calculatePosition(
      final Position basePosition, final String text, final int offset) {
    int line = basePosition.getLine();
    int column = basePosition.getColumn();

    for (int i = 0; i < offset && i < text.length(); i++) {
      final char c = text.charAt(i);
      if (c == '\n') {
        line++;
        column = 0;
      } else if (c == '\r') {
        // Handle \r\n as a single newline.
        if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
          i++; // Skip the \n.
        }
        line++;
        column = 0;
      } else {
        column++;
      }
    }

    return new Position(line, column);
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
