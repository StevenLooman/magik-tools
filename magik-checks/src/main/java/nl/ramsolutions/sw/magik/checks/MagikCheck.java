package nl.ramsolutions.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.MagikVisitor;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.Position;
import nl.ramsolutions.sw.magik.analysis.Range;
import org.sonar.check.RuleProperty;

/**
 * MagikCheck class.
 */
public abstract class MagikCheck extends MagikVisitor {

    private final List<MagikIssue> issues = new ArrayList<>();
    private MagikCheckHolder holder;

    public void setHolder(final MagikCheckHolder holder) {
        this.holder = holder;
    }

    @CheckForNull
    public MagikCheckHolder getHolder() {
        return holder;
    }

    /**
     * Set a parameter.
     *
     * @param name    Key of parameter to set
     * @param value Value of parameter to set
     * @throws IllegalAccessException -
     */
    @SuppressWarnings("java:S3011")
    public void setParameter(final String name, final @Nullable Object value) throws IllegalAccessException {
        boolean found = false;
        for (final Field field : this.getClass().getFields()) {
            final RuleProperty ruleProperty = field.getAnnotation(RuleProperty.class);
            if (ruleProperty == null) {
                continue;
            }

            final String key = ruleProperty.key().replace(" ", "-");
            if (key.equals(name)) {
                field.set(this, value);
                found = true;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Parameter '" + name + "' not found");
        }
    }

    /**
     * Scan the file from the context for issues.
     * @param magikFile File to use.
     * @return List issues.
     */
    public List<MagikIssue> scanFileForIssues(final MagikFile magikFile) {
        this.scanFile(magikFile);
        return Collections.unmodifiableList(this.issues);
    }

    /**
     * Add a new issue.
     * @param location Location of issue.
     * @param message Message of issue.
     */
    public void addIssue(final Location location, final String message) {
        final MagikIssue issue = new MagikIssue(location, message, this);
        this.issues.add(issue);
    }

    /**
     * Add a new issue.
     * @param node AstNode of issue.
     * @param message Message of issue.
     */
    public void addIssue(final AstNode node, final String message) {
        final URI uri = this.getMagikFile().getUri();
        final Range range = Range.fromTree(node);
        final Location location = new Location(uri, range);
        this.addIssue(location, message);
    }

    /**
     * Add a new issue.
     * @param token Token of issue.
     * @param message Message of issue.
     */
    public void addIssue(final Token token, final String message) {
        final URI uri = this.getMagikFile().getUri();
        final Location location = new Location(uri, token);
        this.addIssue(location, message);
    }

    /**
     * Add a new issue.
     * @param startLine Start line of issue.
     * @param startColumn Start column of issue.
     * @param endLine End line of issue.
     * @param endColumn End column of issue.
     * @param message Message of issue.
     */
    public void addIssue(
            final int startLine,
            final int startColumn,
            final int endLine,
            final int endColumn,
            final String message) {
        final URI uri = this.getMagikFile().getUri();
        final Position startPosition = new Position(startLine, startColumn);
        final Position endPosition = new Position(endLine, endColumn);
        final Range range = new Range(startPosition, endPosition);
        final Location location = new Location(uri, range);
        this.addIssue(location, message);
    }

    /**
     * Add a new issue at the file.
     * @param message Message of the issue.
     */
    public void addFileIssue(final String message) {
        final URI uri = this.getMagikFile().getUri();
        final Location location = new Location(uri);
        this.addIssue(location, message);
    }

}
