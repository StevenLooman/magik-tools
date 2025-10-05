package nl.ramsolutions.sw.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.MagikVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.RuleProperty;

/** Magik {@link Check} class. */
public abstract class MagikCheck extends MagikVisitor implements Check {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikCheck.class);

  private CheckHolder holder;
  private IssueCollector issueCollector;

  public void setHolder(final CheckHolder holder) {
    this.holder = holder;
  }

  @CheckForNull
  public CheckHolder getHolder() {
    return holder;
  }

  /**
   * Set a parameter.
   *
   * @param name Key of parameter to set
   * @param value Value of parameter to set
   * @throws IllegalAccessException -
   */
  @SuppressWarnings("java:S3011")
  public void setParameter(final String name, final @Nullable Object value)
      throws IllegalAccessException {
    if (this.holder == null) {
      throw new IllegalStateException();
    }

    boolean found = false;
    for (final Field field : this.getClass().getFields()) {
      final RuleProperty ruleProperty = field.getAnnotation(RuleProperty.class);
      if (ruleProperty == null) {
        continue;
      }

      final String checkKey = this.holder.getCheckKeyKebabCase();
      final String parameterKey = ruleProperty.key().replace(" ", "-");
      final String fullKey = checkKey + "." + parameterKey;
      if (fullKey.equals(name)) {
        LOGGER.debug("Setting parameter for check {}: '{}' to '{}'", checkKey, parameterKey, value);
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
   *
   * @param openedFile File to use.
   * @return List issues.
   */
  public List<Issue> scanFileForIssues(final OpenedFile openedFile) {
    if (!(openedFile instanceof MagikFile magikFile)) {
      throw new IllegalArgumentException("Expected MagikFile, got: " + openedFile.getClass());
    }

    this.issueCollector = new IssueCollector(openedFile, this);
    this.scanFile(magikFile);
    return this.issueCollector.getIssues();
  }

  /**
   * Add a new issue.
   *
   * @param location Location of issue.
   * @param message Message of issue.
   */
  protected void addIssue(final Location location, final String message) {
    this.issueCollector.addIssue(location, message);
  }

  /**
   * Add a new issue.
   *
   * @param node AstNode of issue.
   * @param message Message of issue.
   */
  protected void addIssue(final AstNode node, final String message) {
    this.issueCollector.addIssue(node, message);
  }

  /**
   * Add a new issue.
   *
   * @param token Token of issue.
   * @param message Message of issue.
   */
  protected void addIssue(final Token token, final String message) {
    this.issueCollector.addIssue(token, message);
  }

  /**
   * Add a new issue.
   *
   * @param startLine Start line of issue.
   * @param startColumn Start column of issue.
   * @param endLine End line of issue.
   * @param endColumn End column of issue.
   * @param message Message of issue.
   */
  protected void addIssue(
      final int startLine,
      final int startColumn,
      final int endLine,
      final int endColumn,
      final String message) {
    this.issueCollector.addIssue(startLine, startColumn, endLine, endColumn, message);
  }

  /**
   * Add a new issue at the file.
   *
   * @param message Message of the issue.
   */
  protected void addFileIssue(final String message) {
    this.issueCollector.addFileIssue(message);
  }
}
