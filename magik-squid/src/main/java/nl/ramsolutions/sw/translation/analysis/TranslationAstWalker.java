package nl.ramsolutions.sw.translation.analysis;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import nl.ramsolutions.sw.translation.api.TranslationGrammar;

/** A {@link AstNode} tree walker with pre- and post-methods to iterate a parse tree. */
public abstract class TranslationAstWalker {

  /** Walk children of node. */
  protected void walkChildren(final AstNode node) {
    for (final AstNode childNode : node.getChildren()) {
      this.walkAst(childNode);
    }
  }

  /** Walk trivia and tokens of node. */
  protected void walkTokens(final AstNode tokenNode) {
    // Assume there can be only one token.
    final Token token = tokenNode.getToken();
    token.getTrivia().forEach(this::walkTrivia);
    this.walkToken(token);
  }

  /**
   * Start walking the node.
   *
   * @param node Node to start walking.
   */
  @SuppressWarnings({"java:S1479", "checkstyle:MethodLength"})
  public void walkAst(final AstNode node) {
    final AstNodeType nodeType = node.getType();
    if (!(nodeType instanceof TranslationGrammar value)) {
      this.walkTokens(node);
      return;
    }

    switch (value) {
      case TRANSLATIONS:
        this.walkPreTranslations(node);
        this.walkChildren(node);
        this.walkPostTranslations(node);
        break;

      case TRANSLATION:
        this.walkPreTranslation(node);
        this.walkChildren(node);
        this.walkPostTranslation(node);
        break;

      case TYPE:
        this.walkPreType(node);
        this.walkChildren(node);
        this.walkPostType(node);
        break;

      case NAME:
        this.walkPreName(node);
        this.walkChildren(node);
        this.walkPostName(node);
        break;

      case CONTEXT:
        this.walkPreContext(node);
        this.walkChildren(node);
        this.walkPostContext(node);
        break;

      case LANGUAGE:
        this.walkPreLanguage(node);
        this.walkChildren(node);
        this.walkPostLanguage(node);
        break;

      case SOURCE_TEXT:
        this.walkPreSourceText(node);
        this.walkChildren(node);
        this.walkPostSourceText(node);
        break;

      case TRANSLATED_TEXT:
        this.walkPreTranslatedText(node);
        this.walkChildren(node);
        this.walkPostTranslatedText(node);
        break;

      case IGNORED_FIELD:
        this.walkPreIgnoredField(node);
        this.walkChildren(node);
        this.walkPostIgnoredField(node);
        break;

      case EMPTY_FIELD:
        this.walkPreEmptyField(node);
        this.walkChildren(node);
        this.walkPostEmptyField(node);
        break;

      case FIELD:
        this.walkPreField(node);
        this.walkChildren(node);
        this.walkPostField(node);
        break;

      case SEPARATOR:
        this.walkPreSeparator(node);
        this.walkChildren(node);
        this.walkPostSeparator(node);
        break;

      case NEWLINE:
        this.walkPreNewline(node);
        this.walkChildren(node);
        this.walkPostNewline(node);
        break;

      case COMMENT:
        this.walkPreComment(node);
        this.walkChildren(node);
        this.walkPostComment(node);
        break;

      case SYNTAX_ERROR:
        this.walkPreSyntaxError(node);
        this.walkChildren(node);
        this.walkPostSyntaxError(node);
        break;

      default:
        this.walkPreDefault(node);
        this.walkChildren(node);
        this.walkPostDefault(node);
        break;
    }
  }

  protected void walkPreTranslations(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostTranslations(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreTranslation(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostTranslation(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreType(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostType(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreName(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostName(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreContext(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostContext(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreLanguage(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostLanguage(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreSourceText(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostSourceText(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreTranslatedText(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostTranslatedText(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreIgnoredField(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostIgnoredField(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreEmptyField(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostEmptyField(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreField(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostField(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreSeparator(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostSeparator(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreNewline(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostNewline(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreComment(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostComment(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreSyntaxError(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostSyntaxError(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreDefault(final AstNode node) {
    // pass
  }

  protected void walkPostDefault(final AstNode node) {
    // pass
  }

  protected void walkToken(final Token token) {
    // pass
  }

  protected void walkTrivia(final Trivia trivia) {
    // pass
  }
}
