package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.TemplatedMagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check for a regular expression in a comment.
 */
@TemplatedMagikCheck
@Rule(key = CommentRegularExpressionCheck.CHECK_KEY)
public class CommentRegularExpressionCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "CommentRegularExpression";
    private static final String DEFAULT_REGULAR_EXPRESSION = "";
    private static final String DEFAULT_MESSAGE = "The regular expression matches this comment";

    /**
     * Regular expression to check.
     */
    @RuleProperty(
        key = "regular expression",
        description = "Regular expression to check",
        type = "STRING")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public String regularExpression = DEFAULT_REGULAR_EXPRESSION;

    /**
     * Message to show when the rule matches.
     */
    @RuleProperty(
        key = "message",
        defaultValue = "" + DEFAULT_MESSAGE,
        description = "Message to show when the rule matches",
        type = "STRING")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public String message = DEFAULT_MESSAGE;

    private Pattern pattern;
    private boolean isPatternInitialized;

    @CheckForNull
    @SuppressWarnings("checkstyle:IllegalCatch")
    private Pattern getPattern() {
        if (!this.isPatternInitialized) {
            if (regularExpression != null && !regularExpression.isEmpty()) {
                try {
                    this.pattern = Pattern.compile(regularExpression, Pattern.DOTALL);
                } catch (RuntimeException ex) {
                    throw new IllegalStateException("Unable to compile regular expression: " + regularExpression, ex);
                }
            }
            this.isPatternInitialized = true;
        }

        return this.pattern;
    }

    @Override
    protected void walkToken(final Token token) {
        final Pattern searchPattern = this.getPattern();
        if (searchPattern != null) {
            for (final Trivia trivia : token.getTrivia()) {
                if (!trivia.isComment()) {
                    continue;
                }

                final String originalValue = trivia.getToken().getOriginalValue();
                if (searchPattern.matcher(originalValue).matches()) {
                    this.addIssue(trivia.getToken(), message);
                }
            }
        }
    }

}
