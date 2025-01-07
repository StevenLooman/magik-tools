package nl.ramsolutions.sw.magik.sessionwrapper;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

class MagikJlineHighlighterWalker extends MagikAstWalker {

  private static final AttributedStyle STYLE_CONSTANT =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
  private static final AttributedStyle STYLE_OPERATOR =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
  private static final AttributedStyle STYLE_PUNCTUATOR =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
  private static final AttributedStyle STYLE_KEYWORD =
      AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);

  private static final Set<String> MAGIK_KEYWORDS = Set.of(MagikKeyword.keywordValues());
  private static final Set<String> MAGIK_PUNCTUATORS = Set.of(MagikPunctuator.punctuatorValues());
  private static final Set<String> MAGIK_OPERATORS = Set.of(MagikOperator.operatorValues());

  private final AttributedStringBuilder attributedStringBuilder = new AttributedStringBuilder();

  AttributedString getAttributedString() {
    return this.attributedStringBuilder.toAttributedString();
  }

  private void appendConstant(final AstNode node) {
    final String value = node.getTokenOriginalValue();
    this.attributedStringBuilder.append(value, STYLE_CONSTANT);
  }

  @Override
  protected void walkPostString(final AstNode node) {
    this.appendConstant(node);
  }

  @Override
  protected void walkPostSymbol(final AstNode node) {
    this.appendConstant(node);
  }

  @Override
  protected void walkPostCharacter(final AstNode node) {
    this.appendConstant(node);
  }

  @Override
  protected void walkPostRegexp(final AstNode node) {
    this.appendConstant(node);
  }

  @Override
  protected void walkPostNumber(final AstNode node) {
    this.appendConstant(node);
  }

  @Override
  protected void walkPostIdentifier(final AstNode node) {
    final String value = node.getTokenOriginalValue();
    this.attributedStringBuilder.append(value);
  }

  @Override
  protected void walkPostGlobalRef(final AstNode node) {
    // @ is already added by the punctuator.
    final AstNode labelNode = node.getChildren().get(1);
    final String value = labelNode.getTokenOriginalValue();
    this.attributedStringBuilder.append(value);
  }

  @Override
  protected void walkPostLabel(final AstNode node) {
    // @ is already added by the punctuator.
    final AstNode labelNode = node.getChildren().get(1);
    final String value = labelNode.getTokenOriginalValue();
    this.attributedStringBuilder.append(value);
  }

  @Override
  protected void walkPostSyntaxError(final AstNode node) {
    final String value = node.getTokenOriginalValue();
    this.attributedStringBuilder.append(
        value, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
  }

  @Override
  protected void walkToken(final Token token) {
    final String value = token.getOriginalValue();
    final String valueLowerCase = value.toLowerCase();
    if (MAGIK_KEYWORDS.contains(valueLowerCase)) {
      this.attributedStringBuilder.append(value, STYLE_KEYWORD);
    } else if (MAGIK_PUNCTUATORS.contains(value)) {
      this.attributedStringBuilder.append(value, STYLE_PUNCTUATOR);
    } else if (MAGIK_OPERATORS.contains(value)) {
      this.attributedStringBuilder.append(value, STYLE_OPERATOR);
    }
  }

  private void walkCommentToken(final Token token) {
    final String value = token.getOriginalValue();
    this.attributedStringBuilder.append(
        value, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
  }

  @Override
  protected void walkTrivia(final Trivia trivia) {
    if (trivia.isComment()) {
      trivia.getTokens().forEach(this::walkCommentToken);
    } else {
      trivia.getTokens().stream()
          .map(Token::getOriginalValue)
          .forEach(this.attributedStringBuilder::append);
    }
  }
}
