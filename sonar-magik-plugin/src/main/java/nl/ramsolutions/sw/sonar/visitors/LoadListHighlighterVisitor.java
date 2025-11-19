package nl.ramsolutions.sw.sonar.visitors;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import nl.ramsolutions.sw.loadlist.LoadListVisitor;
import nl.ramsolutions.sw.sonar.TokenLocation;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;

/** load_list.txt/patch_list.txt highlighter visitor. */
public class LoadListHighlighterVisitor extends LoadListVisitor {

  private final NewHighlighting newHighlighting;

  public LoadListHighlighterVisitor(final SensorContext context, final InputFile inputFile) {
    this.newHighlighting = context.newHighlighting();
    this.newHighlighting.onFile(inputFile);
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
