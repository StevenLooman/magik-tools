package nl.ramsolutions.sw.magik.languageserver.semantictokens;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.definitions.analysis.ProductDefAstWalker;
import nl.ramsolutions.sw.definitions.api.SwProductDefinitionKeyword;

/** SwProductDef semantic token walker. */
public class ProductDefSemanticTokenWalker extends ProductDefAstWalker {

  private static final Set<String> KEYWORD_VALUES =
      Arrays.stream(SwProductDefinitionKeyword.values())
          .map(SwProductDefinitionKeyword::getValue)
          .collect(Collectors.toUnmodifiableSet());

  private final List<SemanticToken> semanticTokens = new ArrayList<>();

  public List<SemanticToken> getSemanticTokens() {
    return Collections.unmodifiableList(this.semanticTokens);
  }

  private void addSemanticToken(
      final Token token,
      final SemanticToken.Type type,
      final Set<SemanticToken.Modifier> modifiers) {
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
  protected void walkPostProductName(AstNode node) {
    this.addSemanticToken(node, SemanticToken.Type.CLASS);
  }

  @Override
  protected void walkPostVersionNumber(AstNode node) {
    this.addSemanticToken(node, SemanticToken.Type.NUMBER);
  }

  @Override
  protected void walkPostFreeLine(final AstNode node) {
    this.addSemanticToken(node, SemanticToken.Type.STRING);
  }

  @Override
  protected void walkToken(final Token token) {
    final String value = token.getOriginalValue().toLowerCase();
    if (KEYWORD_VALUES.contains(value)) {
      this.addSemanticToken(token, SemanticToken.Type.KEYWORD);
    }
  }

  @Override
  protected void walkTrivia(final Trivia trivia) {
    if (trivia.isComment()) {
      final Token triviaToken = trivia.getToken();
      this.walkCommentToken(triviaToken);
    }
  }

  private void walkCommentToken(final Token token) {
    this.addSemanticToken(token, SemanticToken.Type.COMMENT);
  }
}
