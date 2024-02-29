package nl.ramsolutions.sw.sonar.visitors;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikVisitor;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.sonar.TokenLocation;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;

/** Magik highlighter visitor. */
public class MagikHighlighterVisitor extends MagikVisitor {

  private static final List<String> KEYWORDS = List.of(MagikKeyword.keywordValues());

  private final NewHighlighting newHighlighting;

  public MagikHighlighterVisitor(final SensorContext context, final InputFile inputFile) {
    this.newHighlighting = context.newHighlighting();
    this.newHighlighting.onFile(inputFile);
  }

  @Override
  protected void walkPostMagik(final AstNode node) {
    this.newHighlighting.save();
  }

  @Override
  protected void walkPreString(final AstNode node) {
    final Token token = node.getToken();
    this.highlight(token, TypeOfText.STRING);
  }

  @Override
  protected void walkPreSymbol(final AstNode node) {
    final Token token = node.getToken();
    this.highlight(token, TypeOfText.CONSTANT);
  }

  @Override
  public void walkToken(final Token token) {
    final String tokenValue = token.getValue();
    final String lowerTokenValue = tokenValue.toLowerCase();
    if (MagikHighlighterVisitor.KEYWORDS.contains(lowerTokenValue)) {
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
