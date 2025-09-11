package nl.ramsolutions.sw.magik.checks.checks;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import org.assertj.core.api.AbstractAssert;

/** {@link MagikCheck} assert. */
public class MagikCheckAssert extends AbstractAssert<MagikCheckAssert, MagikCheck> {

  protected MagikCheckAssert(final MagikCheck actual) {
    super(actual, MagikCheckAssert.class);
  }

  /**
   * Get a new instance for the given {@link MagikCheck}.
   *
   * @param actual {@link MagikCheck} instance.
   * @return Self.
   */
  public static MagikCheckAssert assertThat(final MagikCheck actual) {
    return new MagikCheckAssert(actual);
  }

  /**
   * Test running the {@link MagikCheck} on code results in no issues.
   *
   * @param code Code to run {@link MagikCheck} against.
   * @return Self.
   */
  public MagikCheckAssert reportsNoIssues(final String code) {
    this.isNotNull();

    final List<MagikIssue> issues = this.runCheck(code);
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
  public MagikCheckAssert reportsNoIssues(final Path path)
      throws IllegalArgumentException, IOException {
    this.isNotNull();

    final List<MagikIssue> issues = this.runCheck(path);
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
  public MagikCheckAssert reportsIssueCount(final String code, final int issueCount) {
    this.isNotNull();

    final List<MagikIssue> issues = this.runCheck(code);
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
  public MagikCheckAssert reportsIssueCount(final Path path, final int issueCount)
      throws IllegalArgumentException, IOException {
    this.isNotNull();

    final List<MagikIssue> issues = this.runCheck(path);
    if (issues.size() != issueCount) {
      this.failWithMessage(
          "Expected check to report <%s> issues, but gotten <%s> issues",
          issueCount, issues.size());
    }

    return this;
  }

  private List<MagikIssue> runCheck(final String code) throws IllegalArgumentException {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    return this.actual.scanFileForIssues(magikFile);
  }

  private List<MagikIssue> runCheck(final Path relativePath)
      throws IllegalArgumentException, IOException {
    // Ensure proper path.
    final Path currentPath = Path.of(".").toAbsolutePath().getParent();
    final Path fixedPath =
        currentPath.endsWith("magik-checks")
            ? Path.of("..").resolve(relativePath)
            : Path.of(".").resolve(relativePath);

    final URI uri = fixedPath.toUri();
    final String code = Files.readString(fixedPath);
    final MagikFile magikFile = new MagikFile(uri, code);
    return this.actual.scanFileForIssues(magikFile);
  }
}
