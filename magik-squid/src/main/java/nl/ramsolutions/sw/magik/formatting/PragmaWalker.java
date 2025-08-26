package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.List;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

class PragmaWalker extends FormattingWalker {

  // TODO: This can use a refresh.

  PragmaWalker(final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);
  }

  @Override
  protected void walkPrePragma(final AstNode node) {
    final List<AstNode> childNodes = node.getChildren();
    final AstNode parenNode1 = childNodes.get(1);
    final Token parenToken1 = parenNode1.getToken();
    this.ensureNoWhitespaceBefore(parenToken1);
  }

  @Override
  protected void walkPrePragmaParam(final AstNode node) {
    final Token token = node.getToken();
    final AstNode previousNode = node.getPreviousAstNode();
    final Token previousToken = previousNode != null ? previousNode.getToken() : null;

    if (AstQuery.tokenIs(previousToken, MagikPunctuator.PAREN_L.getValue())) {
      // No leading whitespace before the first pragma param.
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(token);
    } else if (AstQuery.tokenIs(previousToken, MagikPunctuator.COMMA.getValue())) {
      // Leading whitespace before N>0 pragma param.
      this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(token);
    }

    // No leading whitespace before the `=` token.
    final AstNode eqNode = node.getChildren().get(1);
    this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(eqNode);
  }

  @Override
  protected void walkPostPragmaParam(final AstNode node) {
    final AstNode nextNode = node.getNextSibling();
    final Token nextToken = nextNode != null ? nextNode.getToken() : null;

    if (AstQuery.tokenIs(
        nextToken, MagikPunctuator.PAREN_R.getValue(), MagikPunctuator.COMMA.getValue())) {
      // No leading whitespace before the `,`, `)` tokens.
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(nextToken);
    }
  }

  @Override
  protected void walkPrePragmaValue(final AstNode node) {
    // No leading whitespace before the value token.
    this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(node);

    // Only whitespace after `,` tokens within ourselves.
    final List<Token> childTokens = node.getTokens();
    for (int i = 0; i < childTokens.size(); ++i) {
      final Token previousChildToken = i > 0 ? childTokens.get(i - 1) : null;
      final Token childToken = childTokens.get(i);
      if (AstQuery.tokenIs(previousChildToken, MagikPunctuator.COMMA.getValue())) {
        this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(childToken);
      } else {
        this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(childToken);
      }
    }
  }

  @Override
  protected void walkPostPragmaValue(final AstNode node) {
    final AstNode nextNode = node.getNextSibling();
    final Token nextToken = nextNode != null ? nextNode.getToken() : null;

    if (AstQuery.tokenIs(
        nextToken, MagikPunctuator.SQUARE_R.getValue(), MagikPunctuator.COMMA.getValue())) {
      // No leading whitespace before the `,`, `)` or `]` tokens.
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(nextToken);
    }
  }
}
