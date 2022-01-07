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
            String strippedLine = line;

            // Strip \r, if any.
            if (strippedLine.endsWith("\r")) {
                strippedLine = strippedLine.substring(0, line.length() - 1);
            }

            if (strippedLine.endsWith(" ")
                || strippedLine.endsWith("\t")) {
                this.addIssue(lineNo, strippedLine.length(), lineNo, strippedLine.length() + 1, MESSAGE);
            }

            lineNo += 1;
        }
    }

}
