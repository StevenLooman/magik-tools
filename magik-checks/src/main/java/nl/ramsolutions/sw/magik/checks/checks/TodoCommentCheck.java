package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Todo/Fixme/... comment check. // NOSONAR */
@Rule(key = TodoCommentCheck.CHECK_KEY)
public class TodoCommentCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "TodoComment";

  private static final String MESSAGE = "Todo comment: %s.";
  private static final String DEFAULT_FORBIDDEN_WORDS = "TODO,FIXME,HACK,NOTE,TEMP,XXX";

  /** List of comment words, separated by ','. */
  @RuleProperty(
      key = "forbidden comment words",
      defaultValue = "" + DEFAULT_FORBIDDEN_WORDS,
      description = "List of forbidden words, separated by ',', case sensitive",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String forbiddenWords = DEFAULT_FORBIDDEN_WORDS;

  private Collection<String> getForbiddenWords() {
    return Arrays.stream(this.forbiddenWords.split(","))
        .map(String::trim)
        .collect(Collectors.toSet());
  }

  @Override
  protected void walkTrivia(final Trivia trivia) {
    if (!trivia.isComment()) {
      return;
    }

    trivia.getTokens().forEach(this::checkComment);
  }

  private void checkComment(final Token token) {
    final String comment = token.getOriginalValue();
    final String forbiddenWordsRegexp =
        this.getForbiddenWords().stream().collect(Collectors.joining("|", "\\b(", ")\\b"));
    final Pattern pattern = Pattern.compile(forbiddenWordsRegexp);
    final Matcher matcher = pattern.matcher(comment);
    while (matcher.find()) {
      final String word = matcher.group(1);
      final String message = String.format(MESSAGE, word);
      this.addIssue(
          token.getLine(),
          token.getColumn() + matcher.start(1),
          token.getLine(),
          token.getColumn() + matcher.end(1),
          message);
    }
  }
}
