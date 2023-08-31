package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;

/**
 * Check for trailing whitespace.
 */
@Rule(key = TrailingWhitespaceCheck.CHECK_KEY)
public class TrailingWhitespaceCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "TrailingWhitespace";
    private static final String MESSAGE = "Remove the trailing whitespace.";

    @Override
    protected void walkPreMagik(final AstNode node) {
        String[] lines = this.getMagikFile().getSourceLines();
        if (lines == null) {
            lines = new String[]{};
        }

        int lineNo = 1;
        for (final String line : lines) {
            final String trimmedLine = line.endsWith("\r")
                ? line.substring(0, line.length() - 1)  // Strip \r.
                : line;

            if (trimmedLine.endsWith(" ")
                || trimmedLine.endsWith("\t")) {
                final String strippedLine = trimmedLine.stripTrailing();
                this.addIssue(
                    lineNo, strippedLine.length(),
                    lineNo, trimmedLine.length(),
                    MESSAGE);
            }

            lineNo += 1;
        }
    }

}
