package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Todo/Fixme/... comment check.  // NOSONAR
 */
@Rule(key = TodoCommentCheck.CHECK_KEY)
public class TodoCommentCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "CommentTodo";
    private static final String MESSAGE = "Todo comment: %s.";

    private static final String DEFAULT_FORBIDDEN_WORDS = "TODO,FIXME,HACK,NOTE,TEMP,XXX";

    /**
     * List of comment words, separated by ','.
     */
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
        this.getForbiddenWords().stream()
            .forEach(word -> {
                int fromIndex = 0;
                while (fromIndex != -1) {
                    fromIndex = comment.indexOf(word, fromIndex);
                    if (fromIndex == -1) {
                        break;
                    }

                    final String message = String.format(MESSAGE, word);
                    this.addIssue(
                        token.getLine(), token.getColumn() + fromIndex,
                        token.getLine(), token.getColumn() + fromIndex + word.length(),
                        message);

                    fromIndex += word.length();
                }
            });
    }

}
