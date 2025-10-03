package nl.ramsolutions.sw.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;

/** Collector for {@link Issue}s found by {@link Check}s. */
public class IssueCollector {

  private final OpenedFile openedFile;
  private final Check check;
  private final List<Issue> issues = new ArrayList<>();

  /**
   * Constructor.
   *
   * @param openedFile File to which issues belong.
   * @param check Check to which issues belong.
   */
  public IssueCollector(final OpenedFile openedFile, final Check check) {
    this.openedFile = openedFile;
    this.check = check;
  }

  public List<Issue> getIssues() {
    return issues;
  }

  /**
   * Add a new issue.
   *
   * @param location Location of issue.
   * @param message Message of issue.
   */
  protected void addIssue(final Location location, final String message) {
    final Issue issue = new Issue(location, message, this.check);
    this.issues.add(issue);
  }

  /**
   * Add a new issue.
   *
   * @param node AstNode of issue.
   * @param message Message of issue.
   */
  protected void addIssue(final AstNode node, final String message) {
    final URI uri = this.openedFile.getUri();
    final Range range = Range.fromTree(node);
    final Location location = new Location(uri, range);
    this.addIssue(location, message);
  }

  /**
   * Add a new issue.
   *
   * @param token Token of issue.
   * @param message Message of issue.
   */
  protected void addIssue(final Token token, final String message) {
    final URI uri = this.openedFile.getUri();
    final Location location = new Location(uri, token);
    this.addIssue(location, message);
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
    final URI uri = this.openedFile.getUri();
    final Position startPosition = new Position(startLine, startColumn);
    final Position endPosition = new Position(endLine, endColumn);
    final Range range = new Range(startPosition, endPosition);
    final Location location = new Location(uri, range);
    this.addIssue(location, message);
  }

  /**
   * Add a new issue at the file.
   *
   * @param message Message of the issue.
   */
  protected void addFileIssue(final String message) {
    final URI uri = this.openedFile.getUri();
    final Location location = new Location(uri);
    this.addIssue(location, message);
  }
}
