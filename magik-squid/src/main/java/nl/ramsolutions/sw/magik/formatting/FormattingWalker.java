package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;

/** Formatting AST walker which produces {@link TextEdit}s. */
public class FormattingWalker extends MagikAstWalker {

  private final List<TextEdit> textEdits = new ArrayList<>();
  private final PragmaFormattingStrategy pragmaStrategy;
  private final MagikFormattingStrategy magikStrategy;
  private final FinalNewlineStrategy finalNewlineStrategy;
  private FormattingStrategy activeStrategy;

  /**
   * Constructor.
   *
   * @param options Formatting options.
   * @throws IOException -
   */
  public FormattingWalker(final FormattingOptions options) {
    this.pragmaStrategy = new PragmaFormattingStrategy(options);
    this.magikStrategy = new MagikFormattingStrategy(options);
    this.finalNewlineStrategy = new FinalNewlineStrategy(options);
    this.activeStrategy = this.magikStrategy;
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
    return Stream.of(this.pragmaStrategy, this.magikStrategy, this.finalNewlineStrategy);
  }

  // region: AST walker methods.
  @Override
  protected void walkPrePragma(final AstNode node) {
    this.activeStrategy = this.pragmaStrategy;
  }

  @Override
  protected void walkPostPragma(final AstNode node) {
    this.activeStrategy = this.magikStrategy;
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

  // region: Tokens/Trivia walker methods.
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
              final List<TextEdit> strategyTextEdits = strategy.walkWhitespaceToken(token);
              if (strategy == this.activeStrategy) {
                strategyTextEdits.stream().filter(Objects::nonNull).forEach(this.textEdits::add);
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
              final List<TextEdit> strategyTextEdits = strategy.walkCommentToken(token);
              if (strategy == this.activeStrategy) {
                strategyTextEdits.stream().filter(Objects::nonNull).forEach(this.textEdits::add);
              }

              strategy.setLastToken(token);
            });
  }

  private void walkEolToken(final Token token) {
    this.getStrategies()
        .forEach(
            strategy -> {
              final List<TextEdit> strategyTextEdits = strategy.walkEolToken(token);
              if (strategy == this.activeStrategy) {
                strategyTextEdits.stream().filter(Objects::nonNull).forEach(this.textEdits::add);
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
              final List<TextEdit> strategyTextEdits = strategy.walkEofToken(token);
              if (strategy == this.activeStrategy) {
                strategyTextEdits.stream().filter(Objects::nonNull).forEach(this.textEdits::add);
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
              final List<TextEdit> strategyTextEdits = strategy.walkToken(token);
              if (strategy == this.activeStrategy) {
                strategyTextEdits.stream().filter(Objects::nonNull).forEach(this.textEdits::add);
              }

              strategy.setLastToken(token);
            });
  }
  // endregion

}
