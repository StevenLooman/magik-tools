package nl.ramsolutions.sw.magik.formatting.next;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;

class BaseFormattingWalker extends FormattingWalker2 {

  private static final List<String> KEYWORDS =
      Collections.unmodifiableList(List.of(MagikKeyword.keywordValues()));
  private static final List<String> OPERATORS =
      Collections.unmodifiableList(List.of(MagikOperator.operatorValues()));

  BaseFormattingWalker(final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);
  }

  @Override
  protected void walkPostLabel(final AstNode node) {
    this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(node);

    final AstNode labelRegexpNode = node.getChildren().get(1);
    this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(labelRegexpNode);
  }

  @Override
  protected void walkPostGlobalRef(final AstNode node) {
    node.getChildren().stream().skip(1).forEach(this::ensureNoWhitespaceBeforeIfNotFirstTextOnLine);
  }

  @Override
  protected void walkPostSlot(final AstNode node) {
    node.getChildren().stream().skip(1).forEach(this::ensureNoWhitespaceBeforeIfNotFirstTextOnLine);
  }

  @Override
  protected void walkPostSimpleVector(final AstNode node) {
    // Ensure no whitespace after the `{` token.
    final AstNode expressionNode = node.getFirstChild(MagikGrammar.EXPRESSION);
    if (expressionNode != null) {
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(expressionNode);
    }
  }

  @Override
  protected void walkPostAtom(final AstNode node) {
    this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(node);

    final String tokenValue = node.getTokenValue();
    if (tokenValue.equals(MagikPunctuator.PAREN_L.getValue())) {
      // No leading whitespace after the `(` token.
      final AstNode expressionNode = node.getFirstChild(MagikGrammar.EXPRESSION);
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(expressionNode);
    }
  }

  @Override
  protected void walkPostAugmentedAssignmentExpression(final AstNode node) {
    AstQuery.getChildTokens(
            node, MagikOperator.CHEVRON.getValue(), MagikOperator.BOOT_CHEVRON.getValue())
        .forEach(this::ensureNoWhitespaceBeforeIfNotFirstTextOnLine);
  }

  @Override
  protected void walkPostUnaryExpression(final AstNode node) {
    final Token childToken =
        AstQuery.getFirstChildToken(
            node,
            MagikOperator.NOT.getValue(),
            MagikOperator.PLUS.getValue(),
            MagikOperator.MINUS.getValue());
    if (childToken != null) {
      this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(node);

      final AstNode secondChildNode = node.getChildren().get(1);
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(secondChildNode);
    }
  }

  @Override
  protected void walkPostSuper(final AstNode node) {
    final Token lParenToken = AstQuery.getFirstChildToken(node, MagikPunctuator.PAREN_L.getValue());
    final Token rParenToken = AstQuery.getFirstChildToken(node, MagikPunctuator.PAREN_R.getValue());
    if (lParenToken != null) {
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(lParenToken);
    }
    if (rParenToken != null) {
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(rParenToken);
    }

    final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
    if (identifierNode != null) {
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(identifierNode);
    }
  }

  @Override
  protected void walkPostStatementSeparator(final AstNode node) {
    this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(node);
  }

  @Override
  protected void walkPostConditionName(final AstNode node) {
    this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(node);
  }

  @Override
  protected void walkPostIdentifiersWithGather(final AstNode node) {
    final List<AstNode> childNodes = node.getChildren();
    for (int i = 0; i < childNodes.size(); ++i) {
      final AstNode childNode = childNodes.get(i);
      if (childNode.getTokenValue().equals(MagikPunctuator.COMMA.getValue())) {
        this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(childNode);
      } else {
        this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(childNode);
      }
    }
  }

  @Override
  protected void walkPostIdentifier(final AstNode node) {
    this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(node);

    // Fixup identifier by removing whitespaces before/after package.
    final Token token = node.getToken();
    final String originalValue = token.getOriginalValue();
    final String newValue = BaseFormattingWalker.fixIdentifier(originalValue);
    if (!originalValue.equals(newValue)) {
      this.setTokenOriginalValue(token, newValue);
    }
  }

  @Override
  protected void walkPreTuple(final AstNode node) {
    this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(node);
  }

  @Override
  protected void walkPostTuple(final AstNode node) {
    final Token token = node.getToken();
    if (AstQuery.tokenIs(token, MagikPunctuator.PAREN_L.getValue())) {
      // No leading whitespace after the `(` token.
      final List<Token> tokens = node.getTokens();
      if (tokens.size() > 1) {
        final Token secondToken = tokens.get(1);
        this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(secondToken);
      }
    }

    final Token lastToken = node.getLastToken();
    if (AstQuery.tokenIs(lastToken, MagikPunctuator.PAREN_R.getValue())) {
      // No trailing whitespace before the `)` token.
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(lastToken);
    }
  }

  @Override
  protected void walkPostPackageIdentifier(final AstNode node) {
    this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(node);
  }

  @Override
  protected void walkToken(final Token token) {
    final String tokenValue = token.getValue().toLowerCase();
    if (AstQuery.tokenIs(
        token,
        MagikPunctuator.PAREN_R.getValue(),
        MagikPunctuator.BRACE_R.getValue(),
        MagikPunctuator.SQUARE_R.getValue(),
        MagikPunctuator.COMMA.getValue(),
        MagikPunctuator.SEMICOLON.getValue())) {
      this.ensureNoWhitespaceBeforeIfNotFirstTextOnLine(token);
    } else if (BaseFormattingWalker.OPERATORS.contains(tokenValue)
        || BaseFormattingWalker.KEYWORDS.contains(tokenValue)) {
      this.ensureSingleWhitespaceBeforeIfNotFirstTextOnLine(token);
    }
  }

  private static String fixIdentifier(final String value) {
    if (!value.contains(" ")) {
      // No whitespace, so no need to fix.
      return value;
    }

    // If `|`, read until next `|`.
    // Else, read lowercase, skipping whitespace.
    final StringBuilder builder = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); ++i) {
      char chr = value.charAt(i);
      if (chr == '|') { // Piped segment.
        // Skip first `|`.
        ++i;

        // Read until next `|`.
        for (; i < value.length(); ++i) {
          chr = value.charAt(i);
          if (chr == '|') {
            break;
          }

          builder.append(chr);
        }
      } else {
        // Normal character, but skip whitespaces.
        chr = Character.toLowerCase(chr);
        if (Character.isWhitespace(chr)) {
          continue;
        }

        builder.append(chr);
      }
    }

    return builder.toString();
  }
}
