package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.DisabledByDefault;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check if file contains too many methods.
 */
@DisabledByDefault
@Rule(key = FileMethodCountCheck.CHECK_KEY)
public class FileMethodCountCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "FileMethodCount";

    private static final String MESSAGE = "File has too many methods defined (%s/%s).";
    private static final int DEFAULT_MAX_METHOD_COUNT = 10;

    /**
     * Maximum number of methods in a file.
     */
    @RuleProperty(
        key = "method count",
        defaultValue = "" + DEFAULT_MAX_METHOD_COUNT,
        description = "Maximum number of methods in file",
        type = "INTEGER")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public int maxMethodCount = DEFAULT_MAX_METHOD_COUNT;

    @Override
    protected void walkPostMagik(final AstNode node) {
        final long methodCount = node.getChildren(MagikGrammar.METHOD_DEFINITION).stream()
            .count();

        if (methodCount > this.maxMethodCount) {
            final String message = String.format(MESSAGE, methodCount, this.maxMethodCount);
            this.addFileIssue(message);
        }
    }

}
