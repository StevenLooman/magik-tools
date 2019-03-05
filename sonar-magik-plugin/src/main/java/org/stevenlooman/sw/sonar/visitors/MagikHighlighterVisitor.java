package org.stevenlooman.sw.sonar.visitors;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;

import org.stevenlooman.sw.magik.MagikVisitor;
import org.stevenlooman.sw.magik.api.MagikGrammar;
import org.stevenlooman.sw.magik.api.MagikKeyword;
import org.stevenlooman.sw.sonar.TokenLocation;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

public class MagikHighlighterVisitor extends MagikVisitor {
  private static List<String> KEYWORDS = Arrays.asList(MagikKeyword.keywordValues());

  private NewHighlighting newHighlighting;

  public MagikHighlighterVisitor(SensorContext context, InputFile inputFile) {
    newHighlighting = context.newHighlighting();
    newHighlighting.onFile(inputFile);
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(MagikGrammar.STRING, MagikGrammar.SYMBOL);
  }

  @Override
  public void leaveFile(@Nullable AstNode astNode) {
    newHighlighting.save();
  }

  @Override
  public void visitNode(AstNode node) {
    Token token = node.getToken();
    if (node.getType() == MagikGrammar.STRING) {
      highlight(token, TypeOfText.STRING);
    }
    if (node.getType() == MagikGrammar.SYMBOL) {
      highlight(token, TypeOfText.CONSTANT);
    }
  }

  @Override
  public void visitToken(Token token) {
    String tokenValue = token.getValue();
    String lowerTokenValue = tokenValue.toLowerCase();
    if (this.KEYWORDS.contains(lowerTokenValue)) {
      highlight(token, TypeOfText.KEYWORD);
    }

    for (Trivia trivia : token.getTrivia()) {
      if (trivia.isComment()) {
        for (Token triviaToken : trivia.getTokens()) {
          this.highlight(triviaToken, TypeOfText.COMMENT);
        }
      }
    }
  }

  private void highlight(Token token, TypeOfText typeOfText) {
    TokenLocation tokenLocation = new TokenLocation(token);
    newHighlighting.highlight(
        tokenLocation.line(), tokenLocation.column(),
        tokenLocation.endLine(), tokenLocation.endColumn(),
        typeOfText);
  }

}
