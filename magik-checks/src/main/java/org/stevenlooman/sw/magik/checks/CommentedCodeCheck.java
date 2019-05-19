package org.stevenlooman.sw.magik.checks;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;
import org.stevenlooman.sw.magik.parser.MagikParser;

import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

@Rule(key = CommentedCodeCheck.CHECK_KEY)
public class CommentedCodeCheck extends MagikCheck {
  public static final String CHECK_KEY = "CommentedCodeCheck";
  private static final String MESSAGE = "Remove commented code.";

  private static final int DEFAULT_MIN_LINES = 3;
  @RuleProperty(
      key = "min lines",
      defaultValue = "" + DEFAULT_MIN_LINES,
      description = "Minimum number of commented lines")
  public int minLines = DEFAULT_MIN_LINES;

  @Override
  public List<AstNodeType> subscribedTo() {
    return Lists.newArrayList(MagikGrammar.values());
  }

  private void visitComment(Token token, String comment) {
    Charset charset = Charset.forName("iso8859_1");
    MagikParser parser = new MagikParser(charset);
    try {
      parser.parse(comment);
    } catch (RecognitionException exception) {
      return;
    }

    addIssue(MESSAGE, token);
  }

  @Override
  public void visitToken(Token token) {
    List<Trivia> triviaComments = token.getTrivia().stream()
        .filter(trivia -> trivia.isComment())
        .collect(Collectors.toList());
    if (triviaComments.size() < minLines) {
      return;
    }

    String comment = triviaComments.stream()
        .map(trivia -> trivia.getToken().getValue())
        .map(triviaComment -> triviaComment.substring(1))
        .collect(Collectors.joining("\n"));
    visitComment(token, comment);
  }



  private boolean containsCode(String comment) {
    return false;
  }

}
