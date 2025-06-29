package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;

/** Formatting AST walker which produces {@link TextEdit}s. */
public class FormattingWalker extends MagikAstWalker {

  private final List<TextEdit> textEdits = new ArrayList<>();
  private final IndentStrategy indentStrategy;
  private final TrailingWhitespaceStrategy trailingWhitespaceStrategy;
  private final PragmaFormattingStrategy pragmaStrategy;
  private final MagikFormattingStrategy magikStrategy;
  private final FinalNewlineStrategy finalNewlineStrategy;
  private AbstractFormattingStrategy activeStrategy;
  private AstNode currentNode;
  private Token lastTextToken;
  private TokenColumnTracker tokenColumnTracker;

  /**
   * Constructor.
   *
   * @param options Formatting options.
   * @throws IOException -
   */
  public FormattingWalker(final IndentStrategy indentStrategy, final FormattingOptions options) {
    this.indentStrategy = indentStrategy;
    this.trailingWhitespaceStrategy = new TrailingWhitespaceStrategy(options);
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

  private void addTextEdit(final @Nullable TextEdit textEdit) {
    if (textEdit != null) {
      this.textEdits.add(textEdit);

      this.tokenColumnTracker.applyTextEdit(textEdit);
    }
  }

  private Stream<AbstractFormattingStrategy> getStrategies() {
    return Stream.of(this.pragmaStrategy, this.magikStrategy, this.finalNewlineStrategy);
  }

  // region: AST walker methods.
  @Override
  protected void walkPreMagik(final AstNode node) {
    this.tokenColumnTracker = new TokenColumnTracker(node);

    this.walkPreDefault(node);
  }

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
    this.currentNode = node;

    this.getStrategies().forEach(strategy -> strategy.walkPreNode(node));
  }

  @Override
  protected void walkPostDefault(final AstNode node) {
    this.currentNode = this.currentNode.getParent();

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
                strategyTextEdits.forEach(this::addTextEdit);
              }
            });

    this.setLastToken(token);
  }

  private void walkCommentToken(final Token token) {
    // Fixer upper: If comment token contains trailing whitespace,
    // split the token and process separately.
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
                strategyTextEdits.forEach(this::addTextEdit);
              }
            });

    this.handleIndenting(token);

    this.setLastToken(token);
  }

  private void walkEolToken(final Token token) {
    this.handleTrailingWhitespace(token);

    this.getStrategies()
        .forEach(
            strategy -> {
              final List<TextEdit> strategyTextEdits = strategy.walkEolToken(token);
              if (strategy == this.activeStrategy) {
                strategyTextEdits.forEach(this::addTextEdit);
              }
            });

    this.setLastToken(token);
  }

  /**
   * Walk EOF token.
   *
   * @param token EOF token.
   */
  protected void walkEofToken(final Token token) {
    this.handleTrailingWhitespace(token);

    final List<TextEdit> strategyTextEdits = this.finalNewlineStrategy.walkEofToken(token);
    strategyTextEdits.forEach(this::addTextEdit);

    this.setLastToken(token);
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
                strategyTextEdits.forEach(this::addTextEdit);
              }
            });

    this.handleIndenting(token);

    this.setLastToken(token);
  }

  // endregion

  /**
   * Handle indenting.
   *
   * @param token A {@link GenericTokenType.COMMENT} or text-token.
   */
  void handleIndenting(final Token token) {
    if (this.lastTextToken == null || token.isOnSameLineThan(this.lastTextToken)) {
      return;
    }

    final TextEdit textEdit = this.indentStrategy.ensureIndenting(token, this.currentNode);
    this.addTextEdit(textEdit);
  }

  /**
   * Handle trailing whitespace, if any.
   *
   * @param token A {@link GenericTokenType.EOL} of {@link GenericTokenType.EOF} token.
   */
  void handleTrailingWhitespace(final Token token) {
    final TextEdit textEdit = this.trailingWhitespaceStrategy.walkToken(token);
    if (textEdit != null) {
      this.textEdits.add(textEdit);
    }
  }

  /**
   * Set last token.
   *
   * @param token Token to set.
   */
  void setLastToken(final Token token) {
    this.indentStrategy.setLastToken(token);
    this.trailingWhitespaceStrategy.setLastToken(token);
    this.getStrategies().forEach(strategy -> strategy.setLastToken(token));

    if (!AstQuery.tokenIs(
        token, GenericTokenType.WHITESPACE, GenericTokenType.EOL, GenericTokenType.EOF)) {
      this.lastTextToken = token;
    }
  }
}
