package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
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
            final AtomicInteger columnNo = new AtomicInteger(0);
            final AtomicInteger issueColumnNo = new AtomicInteger(0);
            line.chars().forEachOrdered(chr -> {
                final int cur;
                if (chr == '\t') {
                    cur = columnNo.addAndGet(TAB_WIDTH);
                } else {
                    cur = columnNo.incrementAndGet();
                }

                if (cur <= this.maxLineLength + 1) {
                    issueColumnNo.incrementAndGet();
                }
            });

            if (columnNo.get() > this.maxLineLength) {
                final URI uri = this.getMagikFile().getUri();
                final Position startPosition = new Position(lineNo, issueColumnNo.get() - 1);
                final Position endPosition = new Position(lineNo, line.length());
                final Range range = new Range(startPosition, endPosition);
                final Location location = new Location(uri, range);
                final String message = String.format(MESSAGE, line.length(), this.maxLineLength);
                this.addIssue(location, message);
            }

            ++lineNo;
        }
    }
}
