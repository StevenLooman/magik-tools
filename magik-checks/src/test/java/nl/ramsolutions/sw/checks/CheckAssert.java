package nl.ramsolutions.sw.checks;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import org.assertj.core.api.AbstractAssert;

/** Assertions for {@link Check}s. */
public abstract class CheckAssert extends AbstractAssert<CheckAssert, Check> {

  protected CheckAssert(final Check actual) {
    super(actual, CheckAssert.class);
  }

  /**
   * Test running the {@link MagikCheck} on code results in no issues.
   *
   * @param code Code to run {@link MagikCheck} against.
   * @return Self.
   */
  public CheckAssert reportsNoIssues(final String code) {
    this.isNotNull();

    final List<Issue> issues = this.runCheck(code);
    if (!issues.isEmpty()) {
      this.failWithMessage(
          "Expected check to report no issues, but gotten <%s> issues", issues.size());
    }

    return this;
  }

  /**
   * Test running the {@link MagikCheck} on path results in no issues.
   *
   * @param path Path of file to run {@link MagikCheck} against.
   * @return Self.
   * @throws IllegalArgumentException -
   * @throws IOException -
   */
  public CheckAssert reportsNoIssues(final Path path) throws IllegalArgumentException, IOException {
    this.isNotNull();

    final List<Issue> issues = this.runCheck(path);
    if (!issues.isEmpty()) {
      this.failWithMessage(
          "Expected check to report no issues, but gotten <%s> issues", issues.size());
    }

    return this;
  }

  /**
   * Test running the {@link MagikCheck} against code results in a specified number of issues.
   *
   * @param code Code to run {@link MagikCheck} against.
   * @param issueCount Expected issue count.
   * @return Self.
   */
  public CheckAssert reportsIssueCount(final String code, final int issueCount) {
    this.isNotNull();

    final List<Issue> issues = this.runCheck(code);
    if (issues.size() != issueCount) {
      this.failWithMessage(
          "Expected check to report <%s> issues, but gotten <%s> issues",
          issueCount, issues.size());
    }

    return this;
  }

  /**
   * Test running the {@link MagikCheck} on path results in a specified number of issues.
   *
   * @param path Path of file to run {@link MagikCheck} against.
   * @param issueCount Expected issue count.
   * @return Self.
   * @throws IllegalArgumentException -
   * @throws IOException -
   */
  public CheckAssert reportsIssueCount(final Path path, final int issueCount)
      throws IllegalArgumentException, IOException {
    this.isNotNull();

    final List<Issue> issues = this.runCheck(path);
    if (issues.size() != issueCount) {
      this.failWithMessage(
          "Expected check to report <%s> issues, but gotten <%s> issues",
          issueCount, issues.size());
    }

    return this;
  }

  private List<Issue> runCheck(final String code) throws IllegalArgumentException {
    final OpenedFile openedFile = this.createOpenedFile(code);
    return this.actual.scanFileForIssues(openedFile);
  }

  private List<Issue> runCheck(final Path relativePath)
      throws IllegalArgumentException, IOException {
    // Ensure proper path.
    final Path currentPath = Path.of(".").toAbsolutePath().getParent();
    final Path fixedPath =
        currentPath.endsWith("magik-checks")
            ? Path.of("..").resolve(relativePath)
            : Path.of(".").resolve(relativePath);

    final URI uri = fixedPath.toUri();
    final String code = Files.readString(fixedPath);
    final OpenedFile openedFile = this.createOpenedFile(uri, code);
    return this.actual.scanFileForIssues(openedFile);
  }

  /**
   * Create a new instance of the opened file to use.
   *
   * @param code Code of file.
   * @return Opened file instance.
   */
  protected abstract OpenedFile createOpenedFile(final String code);

  /**
   * Create a new instance of the opened file to use.
   *
   * @param uri URI of file.
   * @param code Code of file.
   * @return Opened file instance.
   */
  protected abstract OpenedFile createOpenedFile(final URI uri, final String code);
}
