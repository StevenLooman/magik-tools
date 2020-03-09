package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.TemplatedMagikCheck;

import java.util.regex.Pattern;

@TemplatedMagikCheck
@Rule(key = CommentRegularExpressionCheck.CHECK_KEY)
public class CommentRegularExpressionCheck extends MagikCheck {
  public static final String CHECK_KEY = "CommentRegularExpression";
  private static final String DEFAULT_REGULAR_EXPRESSION = "";
  private static final String DEFAULT_MESSAGE = "The regular expression matches this comment";

  private Pattern pattern = null;
  private boolean isPatternInitialized = false;

  @RuleProperty(
      key = "regularExpression",
      description = "Regular expression to check")
  public String regularExpression = DEFAULT_REGULAR_EXPRESSION;

  @RuleProperty(
      key = "message",
      defaultValue = "" + DEFAULT_MESSAGE,
      description = "Message to show when the rule matches")
  public String message = DEFAULT_MESSAGE;

  private Pattern pattern() {
    if (!isPatternInitialized) {
      if (regularExpression != null && !regularExpression.isEmpty()) {
        try {
          pattern = Pattern.compile(regularExpression, Pattern.DOTALL);
        } catch (RuntimeException ex) {
          throw new IllegalStateException(
              "Unable to compile regular expression: " + regularExpression, ex);
        }
      }
      isPatternInitialized = true;
    }

    return pattern;
  }

  @Override
  protected void walkToken(Token token) {
    Pattern pattern = pattern();
    if (pattern != null) {
      for (Trivia trivia : token.getTrivia()) {
        if (!trivia.isComment()) {
          continue;
        }

        String originalValue = trivia.getToken().getOriginalValue();
        if (pattern.matcher(originalValue).matches()) {
          addIssue(message, trivia.getToken());
        }
      }
    }
  }

}
