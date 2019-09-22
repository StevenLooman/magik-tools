package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;
import org.stevenlooman.sw.magik.parser.MagikParser;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Rule(key = CommentedCodeCheck.CHECK_KEY)
public class CommentedCodeCheck extends MagikCheck {
  public static final String CHECK_KEY = "CommentedCode";
  private static final String MESSAGE = "Remove commented code.";

  private static final int DEFAULT_MIN_LINES = 3;
  @RuleProperty(
      key = "min lines",
      defaultValue = "" + DEFAULT_MIN_LINES,
      description = "Minimum number of commented lines")
  public int minLines = DEFAULT_MIN_LINES;

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(MagikGrammar.BODY);
  }

  @Override
  public void visitNode(AstNode node) {
    Map<Token, String> commentBlocks = extractCommentBlocks(node);
    for (Map.Entry<Token, String> entry : commentBlocks.entrySet()) {
      String comment = entry.getValue();
      if (comment.split("\n").length >= minLines
          && isCommentedCode(comment)) {
        Token token = entry.getKey();
        addIssue(MESSAGE, token);
      }
    }
  }

  private Map<Token, String> extractCommentBlocks(AstNode bodyNode) {
    List<Token> commentTokens = bodyNode.getTokens().stream()
        .flatMap(token -> token.getTrivia().stream())
        .filter(trivia -> trivia.isComment())
        .flatMap(trivia -> trivia.getTokens().stream())
        .collect(Collectors.toList());

    // iterate over all comment tokens and match blocks together
    Map<Token, String> commentBlocks = new Hashtable<>();
    Token startToken = null;
    Token lastToken = null;
    StringBuilder commentBuilder = new StringBuilder();
    for (Token token : commentTokens) {
      if (startToken == null) {
        startToken = token;
      } else if (lastToken.getLine() != token.getLine() - 1
                 || lastToken.getColumn() != token.getColumn()) {
        // save current comment
        commentBlocks.put(startToken, commentBuilder.toString());

        // starting new comment
        startToken = token;
        commentBuilder.setLength(0);
      }

      commentBuilder.append(token.getValue());
      commentBuilder.append("\n");
      lastToken = token;
    }
    // save the last part
    if (startToken != null) {
      commentBlocks.put(startToken, commentBuilder.toString());
    }
    return commentBlocks;
  }

  private boolean isCommentedCode(String comment) {
    String bareComment = comment.replaceAll("^#", "").replaceAll("\n#", "\n");
    Charset charset = Charset.forName("iso8859_1");
    MagikParser parser = new MagikParser(charset);
    try {
      AstNode node = parser.parseSafe(bareComment);
      return node.hasChildren();
    } catch (RecognitionException exception) {
      return false;
    }
  }

}
