package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.Position;
import nl.ramsolutions.sw.magik.analysis.Range;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check for maximum line length.
 */
@Rule(key = LineLengthCheck.CHECK_KEY)
public class LineLengthCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "LineLength";

    private static final String MESSAGE = "Line is too long (%s/%s)";
    private static final int DEFAULT_MAX_LINE_LENGTH = 120;
    private static final int TAB_WIDTH = 8;

    /**
     * Maximum number of characters on a single line.
     */
    @RuleProperty(
        key = "line length",
        defaultValue = "" + DEFAULT_MAX_LINE_LENGTH,
        description = "Maximum number of characters on a single line",
        type = "INTEGER")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public int maxLineLength = DEFAULT_MAX_LINE_LENGTH;

    @Override
    protected void walkPreMagik(final AstNode node) {
        final MagikFile magikFile = this.getMagikFile();
        String[] lines = magikFile.getSourceLines();
        if (lines == null) {
            lines = new String[]{};
        }

        int lineNo = 1;
        for (final String line : lines) {
            int columnNo = 0;
            int issueColumnNo = 0;
            for (int i = 0; i < line.length(); ++i) {
                final char chr = line.charAt(i);
                if (chr == '\t') {
                    final int mod = columnNo % TAB_WIDTH;
                    columnNo += TAB_WIDTH - mod;
                } else {
                    ++columnNo;
                }

                if (columnNo <= this.maxLineLength + 1) {
                    ++issueColumnNo;
                }
            }

            if (columnNo > this.maxLineLength) {
                final URI uri = this.getMagikFile().getUri();
                final Position startPosition = new Position(lineNo, issueColumnNo - 1);
                final Position endPosition = new Position(lineNo, line.length());
                final Range range = new Range(startPosition, endPosition);
                final Location location = new Location(uri, range);
                final String message = String.format(MESSAGE, columnNo, this.maxLineLength);
                this.addIssue(location, message);
            }

            ++lineNo;
        }
    }
}
