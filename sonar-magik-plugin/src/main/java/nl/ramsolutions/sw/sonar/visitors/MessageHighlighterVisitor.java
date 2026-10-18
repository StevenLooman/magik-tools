package nl.ramsolutions.sw.sonar.visitors;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import nl.ramsolutions.sw.message.MessageVisitor;
import nl.ramsolutions.sw.sonar.TokenLocation;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;

/** Message (.msg) file highlighter visitor. */
public class MessageHighlighterVisitor extends MessageVisitor {

  private final NewHighlighting newHighlighting;

  public MessageHighlighterVisitor(final SensorContext context, final InputFile inputFile) {
    this.newHighlighting = context.newHighlighting();
    this.newHighlighting.onFile(inputFile);
  }

  @Override
  protected void walkPostMessages(final AstNode node) {
    this.newHighlighting.save();
  }

  @Override
  public void walkToken(final Token token) {
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
