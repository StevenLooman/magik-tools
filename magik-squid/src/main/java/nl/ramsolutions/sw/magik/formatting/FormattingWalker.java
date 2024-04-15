package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;

/** Formatting AST walker which produces {@link TextEdit}s. */
public class FormattingWalker extends MagikAstWalker {

  private final List<TextEdit> textEdits = new ArrayList<>();
  private final PragmaFormattingStrategy pragmaStrategy;
  private final StandardFormattingStrategy standardStrategy;
  private final FinalNewlineStrategy finalNewlineStrategy;
  private FormattingStrategy activeStrategy;

  /**
   * Constructor.
   *
   * @param options Formatting options.
   * @throws IOException -
   */
  public FormattingWalker(final FormattingOptions options) throws IOException {
    this.pragmaStrategy = new PragmaFormattingStrategy(options);
    this.standardStrategy = new StandardFormattingStrategy(options);
    this.finalNewlineStrategy = new FinalNewlineStrategy(options);
    this.activeStrategy = this.standardStrategy;
  }

  /**
   * Get the edits.
   *
   * @return Edits.
   */
  public List<TextEdit> getTextEdits() {
    return this.textEdits;
  }

  private Stream<FormattingStrategy> getStrategies() {
    return Stream.of(this.pragmaStrategy, this.standardStrategy, this.finalNewlineStrategy);
  }

  // region: Walker methods.
  // region: AST.
  @Override
  protected void walkPrePragma(final AstNode node) {
    this.activeStrategy = this.pragmaStrategy;
  }

  @Override
  protected void walkPostPragma(final AstNode node) {
    this.activeStrategy = this.standardStrategy;
  }

  @Override
  protected void walkPreDefault(final AstNode node) {
    this.getStrategies().forEach(strategy -> strategy.walkPreNode(node));
  }

  @Override
  protected void walkPostDefault(final AstNode node) {
    this.getStrategies().forEach(strategy -> strategy.walkPostNode(node));
  }

  // endregion

  // region: Tokens/Trivia.
  @Override
  protected void walkTrivia(final Trivia trivia) {
    for (final Token token : trivia.getTokens()) {
      if (trivia.isComment()) {
        this.walkCommentToken(token);
      } else if (trivia.isSkippedText()) {
        if (token.getType() == GenericTokenType.EOL) {
          this.walkEolToken(token);
        } else if (token.getType() == GenericTokenType.WHITESPACE) {
          this.walkWhitespaceToken(token);
        }
      }
    }
  }

  /**
   * Walk whitespace token.
   *
   * @param token Whitespace token.
   */
  protected void walkWhitespaceToken(final Token token) {
    this.getStrategies()
        .forEach(
            strategy -> {
              final TextEdit textEdit = strategy.walkWhitespaceToken(token);
              if (textEdit != null && strategy == this.activeStrategy) {
                this.textEdits.add(textEdit);
              }
              strategy.setLastToken(token);
            });
  }

  private void walkCommentToken(final Token token) {
    // Fixer upper: If comment token contains trailing whitespace, split the token and process
    // separately.
    final String comment = token.getOriginalValue();
    final String trimmedComment = comment.stripTrailing();
    if (comment.length() != trimmedComment.length()) {
      final Token commentToken =
          Token.builder(token).setValueAndOriginalValue(trimmedComment).build();
      this.walkCommentToken(commentToken);

      final String trimmed = comment.substring(trimmedComment.length());
      final Token whitespaceToken =
          Token.builder(token)
              .setValueAndOriginalValue(trimmed)
              .setColumn(token.getColumn() + trimmedComment.length())
              .setType(GenericTokenType.WHITESPACE)
              .build();
      this.walkWhitespaceToken(whitespaceToken);

      return;
    }

    this.getStrategies()
        .forEach(
            strategy -> {
              final TextEdit textEdit = strategy.walkCommentToken(token);
              if (textEdit != null && strategy == this.activeStrategy) {
                this.textEdits.add(textEdit);
              }

              strategy.setLastToken(token);
            });
  }

  private void walkEolToken(final Token token) {
    this.getStrategies()
        .forEach(
            strategy -> {
              final TextEdit textEdit = strategy.walkEolToken(token);
              if (textEdit != null && strategy == this.activeStrategy) {
                this.textEdits.add(textEdit);
              }

              strategy.setLastToken(token);
            });
  }

  /**
   * Walk EOF token.
   *
   * @param token EOF token.
   */
  protected void walkEofToken(final Token token) {
    this.activeStrategy = this.finalNewlineStrategy;

    this.getStrategies()
        .forEach(
            strategy -> {
              final TextEdit textEdit = strategy.walkEofToken(token);
              if (textEdit != null && strategy == this.activeStrategy) {
                this.textEdits.add(textEdit);
              }

              strategy.setLastToken(token);
            });
  }

  @Override
  protected void walkToken(final Token token) {
    if (token.getType() == GenericTokenType.EOF) {
      this.walkEofToken(token);
      return;
    }

    this.getStrategies()
        .forEach(
            strategy -> {
              final TextEdit textEdit = strategy.walkToken(token);
              if (textEdit != null && strategy == this.activeStrategy) {
                this.textEdits.add(textEdit);
              }

              strategy.setLastToken(token);
            });
  }
  // endregion
  // endregion

}
