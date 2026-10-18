package nl.ramsolutions.sw.magik.languageserver.semantictokens;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.translation.analysis.TranslationAstWalker;

/** Translation semantic token walker. */
public class TranslationSemanticTokenWalker extends TranslationAstWalker {

  private final List<SemanticToken> semanticTokens = new ArrayList<>();
  private int syntaxErrorDepth;

  public List<SemanticToken> getSemanticTokens() {
    return Collections.unmodifiableList(this.semanticTokens);
  }

  private void addSemanticToken(
      final Token token,
      final SemanticToken.Type type,
      final Set<SemanticToken.Modifier> modifiers) {
    if (this.syntaxErrorDepth > 0) {
      return;
    }

    final SemanticToken semanticToken = new SemanticToken(token, type, modifiers);
    this.semanticTokens.add(semanticToken);
  }

  private void addSemanticToken(final Token token, final SemanticToken.Type type) {
    Set<SemanticToken.Modifier> modifiers = Collections.emptySet();
    this.addSemanticToken(token, type, modifiers);
  }

  private void addSemanticToken(final AstNode node, final SemanticToken.Type type) {
    final Token token = node.getToken();
    this.addSemanticToken(token, type);
  }

  @Override
  protected void walkPostType(final AstNode node) {
    this.addSemanticToken(node, SemanticToken.Type.KEYWORD);
  }

  @Override
  protected void walkPostName(final AstNode node) {
    this.addSemanticToken(node, SemanticToken.Type.STRING);
  }

  @Override
  protected void walkPostContext(final AstNode node) {
    this.addSemanticToken(node, SemanticToken.Type.STRING);
  }

  @Override
  protected void walkPostLanguage(final AstNode node) {
    this.addSemanticToken(node, SemanticToken.Type.STRING);
  }

  @Override
  protected void walkTrivia(final Trivia trivia) {
    if (trivia.isComment()) {
      final Token triviaToken = trivia.getToken();
      this.walkCommentToken(triviaToken);
    }
  }

  private void walkCommentToken(final Token token) {
    final SemanticToken semanticToken =
        new SemanticToken(token, SemanticToken.Type.COMMENT, Collections.emptySet());
    this.semanticTokens.add(semanticToken);
  }

  @Override
  protected void walkPreSyntaxError(final AstNode node) {
    this.syntaxErrorDepth++;
  }

  @Override
  protected void walkPostSyntaxError(final AstNode node) {
    this.syntaxErrorDepth--;
  }
}
