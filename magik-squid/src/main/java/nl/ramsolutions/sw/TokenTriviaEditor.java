package nl.ramsolutions.sw;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A utility class for editing {@link Trivia} of {@link Token}s in an {@link AstNode}. */
public class TokenTriviaEditor {

  /** Map of line number to list of {@link Token}s on that line, including trivia tokens. */
  private final Map<Integer, List<Token>> lineTokens;

  /**
   * Map of trivia {@link Token} to the parent {@link Token} that has this trivia. Only contains
   * entries for trivia tokens that are actually present in the AST.
   */
  private final Map<Token, Token> triviaTokenTokens;

  /**
   * Constructor that initializes the TokenEditor with the {@link Token}s from the given {@link
   * AstNode}.
   *
   * @param node The {@link AstNode} containing the {@link Token}s to be edited.
   */
  public TokenTriviaEditor(final AstNode node) {
    this.lineTokens =
        node.getTokens().stream()
            .flatMap(
                token -> {
                  return Stream.concat(
                      // Trivia tokens come before the token itself.
                      token.getTrivia().stream().flatMap(trivia -> trivia.getTokens().stream()),
                      Stream.of(token));
                })
            .collect(
                Collectors.groupingBy(
                    Token::getLine,
                    Collectors.mapping(token -> token, Collectors.toCollection(ArrayList::new))));
    this.triviaTokenTokens =
        node.getTokens().stream()
            .flatMap(
                token ->
                    token.getTrivia().stream()
                        .map(Trivia::getToken)
                        .map(triviaToken -> Map.entry(triviaToken, token)))
            .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

    // Ensure every physical line from 1 up to the highest line has an entry, even lines whose
    // tokens are absent from the AST caused by a syntax error.
    final int maxLine = this.lineTokens.keySet().stream().max(Integer::compareTo).orElse(0);
    for (int line = 1; line <= maxLine; ++line) {
      this.lineTokens.computeIfAbsent(line, key -> new ArrayList<>());
    }
  }

  /**
   * Gets the {@link Token} before the specified {@link Token} on the same line.
   *
   * @param token The {@link Token} to find the previous token for.
   * @return The previous {@link Token} on the same line, or null if there is no previous token.
   */
  @CheckForNull
  public Token getTokenBeforeOnSameLine(final Token token) {
    final int tokenLine = token.getLine();
    final List<Token> tokensOnLine = this.lineTokens.get(tokenLine);
    if (tokensOnLine == null) {
      throw new IllegalArgumentException("No tokens found on line: " + tokenLine);
    }

    // Find the index of the token in the line
    final int tokensOnLineIndex = tokensOnLine.indexOf(token);
    if (tokensOnLineIndex < 0) {
      throw new IllegalArgumentException("Token not found on line: " + tokenLine);
    }

    if (tokensOnLineIndex == 0) {
      // No token before this one.
      return null;
    }

    // Return the token before the specified token.
    return tokensOnLine.get(tokensOnLineIndex - 1);
  }

  /**
   * Gets the {@link Token} before the specified {@link Token} on the same line, or the last token
   * on the previous line if there is no token before it on the same line.
   *
   * @param token The {@link Token} to find the previous token for.
   * @return The previous {@link Token} on the same line, or the last token on the previous line if
   *     there is no token before it on the same line.
   */
  @CheckForNull
  public Token getTokenBefore(final Token token) {
    final Token tokenBefore = this.getTokenBeforeOnSameLine(token);
    if (tokenBefore != null) {
      return tokenBefore;
    }

    // Keep on searching backwards until we find a token on a previous line.
    final int tokenLine = token.getLine();
    for (int tokenLineBefore = tokenLine - 1; tokenLineBefore >= 1; --tokenLineBefore) {
      final List<Token> tokensOnLineBefore = this.lineTokens.get(tokenLineBefore);
      if (tokensOnLineBefore != null) {
        // Return the last token on the previous line.
        final int lastIndex = tokensOnLineBefore.size() - 1;
        if (lastIndex < 0) {
          // No tokens on this line, keep searching.
          // Can happen with syntax errors.
          continue;
        }

        return tokensOnLineBefore.get(lastIndex);
      }
    }

    // Return the last token on the previous line.
    return null;
  }

  /**
   * Sets the original value of the specified {@link Token} to the new value.
   *
   * @param token The {@link Token} whose original value is to be set.
   * @param newValue The new original value to set for the {@link Token}.
   */
  public Token setTokenOriginalValue(final Token token, final String newValue) {
    final String originalValue = token.getOriginalValue();
    if (originalValue.contains("\n")
        || originalValue.contains("\r")
        || newValue.contains("\n")
        || newValue.contains("\r")) {
      throw new IllegalArgumentException("New value cannot contain newline characters");
    }

    if (originalValue.equals(newValue)) {
      return token;
    }

    TokenHelper.setOriginalValue(token, newValue);

    // Update the tokens after this token on the same line.
    final int tokenLine = token.getLine();
    final List<Token> tokensOnLine = this.lineTokens.get(tokenLine);
    if (tokensOnLine == null) {
      throw new IllegalArgumentException("No tokens found on line: " + tokenLine);
    }

    // Find the index of the token in the line.
    final int tokensOnLineIndex = tokensOnLine.indexOf(token);
    if (tokensOnLineIndex < 0) {
      throw new IllegalArgumentException("Token not found on line: " + tokenLine);
    }

    // Shift all tokens after the specified token with columnOffset.
    final int columnOffset = newValue.length() - originalValue.length();
    final List<Token> tokensAfter =
        tokensOnLine.subList(tokensOnLineIndex + 1, tokensOnLine.size());
    tokensAfter.forEach(
        tokenAfter -> TokenHelper.setColumn(tokenAfter, tokenAfter.getColumn() + columnOffset));

    return token;
  }

  /**
   * Adds whitespace {@link Trivia}+{@link Token} before the specified token, on the location of the
   * given {@link token}.
   *
   * <p>This updates any tokens following the specified token to account for the added whitespace
   * trivia.
   *
   * @param token The {@link Token} or {@link Trivia} {@link Token} which to add the whitespace at.
   * @throws IllegalArgumentException if the specified token is not found on the line.
   */
  public Token addWhitespaceBefore(final Token token, final String whitespace) {
    final int tokenLine = token.getLine();
    final List<Token> tokensOnLine = this.lineTokens.get(tokenLine);
    if (tokensOnLine == null) {
      throw new IllegalArgumentException("No tokens found on line: " + tokenLine);
    }

    // Find the index of the token in the line
    final int tokensOnLineIndex = tokensOnLine.indexOf(token);
    if (tokensOnLineIndex < 0) {
      throw new IllegalArgumentException("Token not found on line: " + tokenLine);
    }

    // Ensure the whitespace is not empty.
    if (whitespace.isEmpty()) {
      throw new IllegalArgumentException("Whitespace cannot be empty");
    }

    // Ensure there isn't already whitespace before the token.
    final Token tokenBefore = this.getTokenBefore(token);
    if (token.getType().equals(GenericTokenType.WHITESPACE)) {
      throw new IllegalArgumentException("Cannot add whitespace before a whitespace token");
    }

    if (tokenBefore != null && tokenBefore.getType().equals(GenericTokenType.WHITESPACE)) {
      throw new IllegalArgumentException("There is already whitespace before the token: " + token);
    }

    // Create a new Trivia, and add it as the last Trivia of the Token.
    final Token realToken = this.triviaTokenTokens.getOrDefault(token, token);
    final Trivia beforeTrivia =
        realToken.getTrivia().stream()
            .filter(trivia -> trivia.getToken().equals(token))
            .findFirst()
            .orElse(null);
    final Trivia whitespaceTrivia =
        Trivia.createSkippedText(
            Token.builder()
                .setType(GenericTokenType.WHITESPACE)
                .setValueAndOriginalValue(whitespace)
                .setLine(token.getLine())
                .setColumn(token.getColumn())
                .setURI(token.getURI())
                .build());
    TokenHelper.addTrivia(realToken, beforeTrivia, whitespaceTrivia);

    // Shift all tokens after the specified token with columnOffset.
    final int columnOffset = whitespace.length();
    final List<Token> tokensAfter = tokensOnLine.subList(tokensOnLineIndex, tokensOnLine.size());
    tokensAfter.forEach(
        tokenAfter -> TokenHelper.setColumn(tokenAfter, tokenAfter.getColumn() + columnOffset));

    // Add new token to tokensOnLine.
    final Token whitespaceToken = whitespaceTrivia.getToken();
    tokensOnLine.add(tokensOnLineIndex, whitespaceToken);

    // Add new trivia token to triviaTokenTokens.
    this.triviaTokenTokens.put(whitespaceToken, realToken);

    return whitespaceToken;
  }

  /**
   * Remove a whitespace {@link Trivia} {@link Token} from the parent token.
   *
   * <p>This updates any tokens following the specified token to account for the removed whitespace
   * trivia.
   *
   * @param whitespaceToken The {@link Trivia} {@link Token} to remove the whitespace trivia.
   */
  public void removeWhitespaceToken(final Token whitespaceToken) {
    if (!whitespaceToken.getType().equals(GenericTokenType.WHITESPACE)) {
      throw new IllegalArgumentException("Only whitespace trivia tokens are supported");
    }

    final int triviaTokenLine = whitespaceToken.getLine();
    final List<Token> tokensOnLine = this.lineTokens.get(triviaTokenLine);
    if (tokensOnLine == null) {
      throw new IllegalArgumentException("No tokens found on line: " + triviaTokenLine);
    }

    // Find the index of the token in the line.
    final int tokensOnLineIndex = tokensOnLine.indexOf(whitespaceToken);
    if (tokensOnLineIndex < 0) {
      throw new IllegalArgumentException("Token not found on line: " + triviaTokenLine);
    }

    // Find the parent token for the given triviaToken, and remove the trivia for triviaToken.
    final Token parentToken = this.triviaTokenTokens.get(whitespaceToken);
    Objects.requireNonNull(parentToken);
    final Trivia trivia =
        parentToken.getTrivia().stream()
            .filter(searchTrivia -> searchTrivia.getToken().equals(whitespaceToken))
            .findFirst()
            .orElseThrow();
    TokenHelper.removeTrivia(parentToken, trivia);

    // Shift all tokens after the specified token with columnOffset.
    final String triviaTokenValue = whitespaceToken.getOriginalValue();
    final int columnOffset = -triviaTokenValue.length();
    final List<Token> tokensAfter = tokensOnLine.subList(tokensOnLineIndex, tokensOnLine.size());
    tokensAfter.forEach(
        tokenAfter -> TokenHelper.setColumn(tokenAfter, tokenAfter.getColumn() + columnOffset));

    // Remove old token from tokensOnLine.
    tokensOnLine.remove(whitespaceToken);

    // Remove trivia token from triviaTokenTokens.
    this.triviaTokenTokens.remove(whitespaceToken);
  }

  /**
   * Remove a EOL {@link Trivia} {@link Token} from the parent token.
   *
   * <p>This updates any tokens following the specified token to account for the removed whitespace
   * trivia.
   *
   * @param eolToken The EOL token to remove.
   */
  public void removeEolToken(final Token eolToken) {
    if (!eolToken.getType().equals(GenericTokenType.EOL)) {
      throw new IllegalArgumentException("Only EOL trivia tokens are supported");
    }

    final int triviaTokenLine = eolToken.getLine();
    final List<Token> tokensOnLine = this.lineTokens.get(triviaTokenLine);
    if (tokensOnLine == null) {
      throw new IllegalArgumentException("No tokens found on line: " + triviaTokenLine);
    }

    // Find the index of the token in the line.
    final int tokensOnLineIndex = tokensOnLine.indexOf(eolToken);
    if (tokensOnLineIndex < 0) {
      throw new IllegalArgumentException("Token not found on line: " + triviaTokenLine);
    }

    // Find the parent token for the given triviaToken, and remove the trivia for triviaToken.
    final Token parentToken = this.triviaTokenTokens.get(eolToken);
    Objects.requireNonNull(parentToken);
    final Trivia trivia =
        parentToken.getTrivia().stream()
            .filter(searchTrivia -> searchTrivia.getToken().equals(eolToken))
            .findFirst()
            .orElseThrow();
    TokenHelper.removeTrivia(parentToken, trivia);

    // Assume the EOL token is the last token on the line. Fair assumption, no?
    // Also, assume there is another line after the trivia token... i.e., parentToken.

    // Get tokens from next line.
    final int nextLine = triviaTokenLine + 1;
    if (nextLine > this.lineTokens.size()) {
      // No next line, so nothing to do.
      return;
    }

    // Shift all tokens from next line after the specified trivia token with
    // columnOffset.
    // Update the line for all next line tokens.
    final List<Token> nextLineTokens = this.lineTokens.get(nextLine);
    final int columnOffset = eolToken.getColumn();
    nextLineTokens.forEach(
        nextLineToken -> {
          TokenHelper.setColumn(nextLineToken, nextLineToken.getColumn() + columnOffset);
          TokenHelper.setLine(nextLineToken, triviaTokenLine);
        });

    // Merge tokens on current line with next line.
    final List<Token> tokensBefore = tokensOnLine.subList(0, tokensOnLineIndex);
    final List<Token> newLineTokens =
        Stream.concat(tokensBefore.stream(), nextLineTokens.stream())
            .collect(Collectors.toCollection(ArrayList::new));
    this.lineTokens.put(triviaTokenLine, newLineTokens);
    this.lineTokens.remove(nextLine);

    // Move all lineTokens after this token "up" one line.
    final int totalSize = this.lineTokens.size() + 1;
    for (int currentLine = triviaTokenLine + 2; currentLine <= totalSize; ++currentLine) {
      if (!this.lineTokens.containsKey(currentLine)) {
        // Can happen due to syntax errors.
        continue;
      }

      final List<Token> followingLineTokens = this.lineTokens.get(currentLine);
      final int prevLine = currentLine - 1;
      this.lineTokens.put(prevLine, followingLineTokens);
      this.lineTokens.remove(currentLine);

      // Update line in following tokens.
      followingLineTokens.forEach(
          followingLineToken -> TokenHelper.setLine(followingLineToken, prevLine));
    }

    // this.assertLinesConnected();

    // Remove trivia token from triviaTokenTokens.
    this.triviaTokenTokens.remove(eolToken);
  }

  /**
   * Add an EOL {@link Trivia}+{@link Token} before the specified token, on the location of the
   * given {@link token}.
   *
   * <p>This updates any tokens following the specified token to account for the added EOL token.
   *
   * @param token Token to add {@link Trivia} {@link Token} to.
   * @param eol Newline to add (`\n` or `\r\n` usually.)
   */
  public Token addEolBefore(final Token token, final String eol) {
    final int tokenLine = token.getLine();
    final List<Token> tokensOnLine = this.lineTokens.get(tokenLine);
    if (tokensOnLine == null) {
      throw new IllegalArgumentException("No tokens found on line: " + tokenLine);
    }

    // Find the index of the token in the line
    final int tokensOnLineIndex = tokensOnLine.indexOf(token);
    if (tokensOnLineIndex < 0) {
      throw new IllegalArgumentException("Token not found on line: " + tokenLine);
    }

    // Create a new Trivia, and add it as the last Trivia of the Token.
    final Token realToken = this.triviaTokenTokens.getOrDefault(token, token);
    final Trivia beforeTrivia =
        realToken.getTrivia().stream()
            .filter(trivia -> trivia.getToken().equals(token))
            .findFirst()
            .orElse(null);
    final Trivia eolTrivia =
        Trivia.createSkippedText(
            Token.builder()
                .setType(GenericTokenType.EOL)
                .setValueAndOriginalValue(eol)
                .setLine(token.getLine())
                .setColumn(token.getColumn())
                .setURI(token.getURI())
                .build());
    TokenHelper.addTrivia(realToken, beforeTrivia, eolTrivia);

    final List<Token> tokensBefore = tokensOnLine.subList(0, tokensOnLineIndex);
    final List<Token> tokensAfter = tokensOnLine.subList(tokensOnLineIndex, tokensOnLine.size());

    // Shift token + token after with (negative) columnOffset, so that these begin
    // at column 0.
    final int columnOffset = -token.getColumn();
    tokensAfter.forEach(
        tokenAfter -> TokenHelper.setColumn(tokenAfter, tokenAfter.getColumn() + columnOffset));

    // Add new EOL token to new line tokens.
    final Token eolToken = eolTrivia.getToken();
    final List<Token> newTokensBefore =
        Stream.concat(tokensBefore.stream(), Stream.of(eolToken))
            .collect(Collectors.toCollection(ArrayList::new));

    // Move all lineTokens after this token "down" one line.
    for (int currentLine = this.lineTokens.size(); currentLine > tokenLine; --currentLine) {
      if (!this.lineTokens.containsKey(currentLine)) {
        // Can happen due to syntax errors.
        continue;
      }

      final List<Token> followingLineTokens = this.lineTokens.get(currentLine);
      this.lineTokens.put(currentLine + 1, followingLineTokens);
      this.lineTokens.remove(currentLine);
    }

    // Update line tokens.
    this.lineTokens.put(tokenLine, newTokensBefore);
    this.lineTokens.put(tokenLine + 1, tokensAfter); // TODO: Doesn't this overwrite the next line?

    // Add new trivia token to triviaTokenTokens.
    this.triviaTokenTokens.put(eolToken, realToken);

    // Shift specified token and all tokens after the specified token with
    // lineOffset (1).
    for (int currentLine = this.lineTokens.size(); currentLine > tokenLine; --currentLine) {
      final List<Token> followingLineTokens = this.lineTokens.get(currentLine);
      if (followingLineTokens == null) {
        // Can happen due to syntax errors.
        continue;
      }

      followingLineTokens.forEach(
          followingLineToken -> {
            final int line = followingLineToken.getLine();
            final int newLine = line + 1;
            TokenHelper.setLine(followingLineToken, newLine);
          });
    }

    return eolToken;
  }

  /**
   * Get the line separator used in this file.
   *
   * @return The used line separator in this file.
   */
  public String getLineSeparator() {
    // Scan for the first newline in this file, and use that.
    final List<Token> firstLineTokens = this.lineTokens.get(1);
    if (firstLineTokens != null && !firstLineTokens.isEmpty()) {
      final Token lastToken = firstLineTokens.get(firstLineTokens.size() - 1);
      final TokenType lastTokenType = lastToken.getType();
      if (lastTokenType.equals(GenericTokenType.EOL)) {
        return lastToken.getOriginalValue();
      }
    }

    // Otherwise, newline.
    return "\n"; // TODO: System.lineSeparator() ?
  }

  // /**
  //  * Debug method to assert that all lines from 1 to max line have at least one token.
  //  */
  // private void assertLinesConnected() {
  //   final int min = 1;
  //   final int max = this.lineTokens.keySet().stream().max(Integer::compareTo).orElseThrow();
  //   for (int line = min; line <= max; ++line) {
  //     final List<Token> tokens = this.lineTokens.get(line);
  //     if (tokens == null || tokens.isEmpty()) {
  //       System.err.println("Line " + line + " has no tokens, but should have at least one.");
  //     }
  //   }
  // }

  // /**
  //  * Debug method to get the full text represented by the current tokens.
  //  *
  //  * @return The full text.
  //  */
  // private String getText() {
  //   return this.lineTokens.keySet().stream()
  //       .sorted()
  //       .flatMap(line -> this.lineTokens.get(line).stream())
  //       .map(Token::getOriginalValue)
  //       .collect(Collectors.joining());
  // }
}
