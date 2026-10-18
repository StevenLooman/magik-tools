package nl.ramsolutions.sw.message.analysis;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import nl.ramsolutions.sw.message.api.MessageGrammar;

/** A {@link AstNode} tree walker with pre- and post-methods to iterate a parse tree. */
public abstract class MessageAstWalker {

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
    if (!(nodeType instanceof MessageGrammar value)) {
      this.walkTokens(node);
      return;
    }

    switch (value) {
      case MESSAGES:
        this.walkPreMessages(node);
        this.walkChildren(node);
        this.walkPostMessages(node);
        break;

      case USES:
        this.walkPreUses(node);
        this.walkChildren(node);
        this.walkPostUses(node);
        break;

      case USE_NAME:
        this.walkPreUseName(node);
        this.walkChildren(node);
        this.walkPostUseName(node);
        break;

      case MESSAGE:
        this.walkPreMessage(node);
        this.walkChildren(node);
        this.walkPostMessage(node);
        break;

      case MESSAGE_ID:
        this.walkPreMessageId(node);
        this.walkChildren(node);
        this.walkPostMessageId(node);
        break;

      case LANGUAGE:
        this.walkPreLanguage(node);
        this.walkChildren(node);
        this.walkPostLanguage(node);
        break;

      case REMOVE:
        this.walkPreRemove(node);
        this.walkChildren(node);
        this.walkPostRemove(node);
        break;

      case IGNORED_TEXT:
        this.walkPreIgnoredText(node);
        this.walkChildren(node);
        this.walkPostIgnoredText(node);
        break;

      case MESSAGE_TEXT:
        this.walkPreMessageText(node);
        this.walkChildren(node);
        this.walkPostMessageText(node);
        break;

      case TEXT_LINE:
        this.walkPreTextLine(node);
        this.walkChildren(node);
        this.walkPostTextLine(node);
        break;

      case TEXT:
        this.walkPreText(node);
        this.walkChildren(node);
        this.walkPostText(node);
        break;

      case PARAMETER:
        this.walkPreParameter(node);
        this.walkChildren(node);
        this.walkPostParameter(node);
        break;

      case ESCAPE:
        this.walkPreEscape(node);
        this.walkChildren(node);
        this.walkPostEscape(node);
        break;

      case INVALID_ESCAPE:
        this.walkPreInvalidEscape(node);
        this.walkChildren(node);
        this.walkPostInvalidEscape(node);
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

  protected void walkPreMessages(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostMessages(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreUses(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostUses(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreUseName(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostUseName(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreMessage(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostMessage(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreMessageId(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostMessageId(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreLanguage(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostLanguage(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreRemove(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostRemove(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreIgnoredText(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostIgnoredText(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreMessageText(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostMessageText(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreTextLine(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostTextLine(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreText(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostText(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreParameter(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostParameter(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreEscape(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostEscape(final AstNode node) {
    this.walkPostDefault(node);
  }

  protected void walkPreInvalidEscape(final AstNode node) {
    this.walkPreDefault(node);
  }

  protected void walkPostInvalidEscape(final AstNode node) {
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
