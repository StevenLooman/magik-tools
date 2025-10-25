package nl.ramsolutions.sw.sonar.visitors;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.List;
import nl.ramsolutions.sw.productdef.ProductDefVisitor;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionKeyword;
import nl.ramsolutions.sw.sonar.TokenLocation;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;

/** product.def highlighter visitor. */
public class ProductDefHighlighterVisitor extends ProductDefVisitor {

  private static final List<String> KEYWORDS = List.of(ProductDefinitionKeyword.keywordValues());

  private final NewHighlighting newHighlighting;

  public ProductDefHighlighterVisitor(final SensorContext context, final InputFile inputFile) {
    this.newHighlighting = context.newHighlighting();
    this.newHighlighting.onFile(inputFile);
  }

  @Override
  protected void walkPostProductDefinition(final AstNode node) {
    this.newHighlighting.save();
  }

  @Override
  protected void walkPreFreeLine(final AstNode node) {
    final Token token = node.getToken();
    final String tokenValue = token.getValue();
    if (tokenValue.isBlank()) {
      return;
    }

    this.highlight(token, TypeOfText.STRING);
  }

  @Override
  public void walkToken(final Token token) {
    final String tokenValue = token.getValue();
    final String lowerTokenValue = tokenValue.toLowerCase();
    if (ProductDefHighlighterVisitor.KEYWORDS.contains(lowerTokenValue)) {
      this.highlight(token, TypeOfText.KEYWORD);
    }

    for (final Trivia trivia : token.getTrivia()) {
      if (trivia.isComment()) {
        for (final Token triviaToken : trivia.getTokens()) {
          this.highlight(triviaToken, TypeOfText.COMMENT);
        }
      }
    }
  }

  private void highlight(final Token token, final TypeOfText typeOfText) {
    final TokenLocation tokenLocation = new TokenLocation(token);
    this.newHighlighting.highlight(
        tokenLocation.line(),
        tokenLocation.column(),
        tokenLocation.endLine(),
        tokenLocation.endColumn(),
        typeOfText);
  }
}
