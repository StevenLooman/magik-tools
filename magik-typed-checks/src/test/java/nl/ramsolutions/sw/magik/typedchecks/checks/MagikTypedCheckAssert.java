package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.assertj.core.api.AbstractAssert;

/**
 * Assertions for {@link MagikTypedCheck} instances.
 *
 * <p>Use via {@link #assertThat(MagikTypedCheck)}.
 */
public class MagikTypedCheckAssert extends AbstractAssert<MagikTypedCheckAssert, MagikTypedCheck> {

  protected MagikTypedCheckAssert(final MagikTypedCheck actual) {
    super(actual, MagikTypedCheckAssert.class);
  }

  /**
   * Get a new instance for the given {@link MagikTypedCheck}.
   *
   * @param actual {@link MagikTypedCheck} instance.
   * @return Self.
   */
  public static MagikTypedCheckAssert assertThat(final MagikTypedCheck actual) {
    return new MagikTypedCheckAssert(actual);
  }

  /**
   * Test running the {@link MagikCheck} on code results in no issues.
   *
   * @param code Code to run {@link MagikCheck} against.
   * @return Self.
   */
  public MagikTypedCheckAssert reportsNoIssues(
      final String code, final IDefinitionKeeper definitionKeeper) {
    this.isNotNull();

    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper);
    if (!issues.isEmpty()) {
      this.failWithMessage(
          "Expected check to report no issues, but gotten <%s> issues", issues.size());
    }

    return this;
  }

  /**
   * Test running the {@link MagikTypedCheck} on path results in no issues.
   *
   * @param path Path of file to run {@link MagikTypedCheck} against.
   * @return Self.
   * @throws IllegalArgumentException -
   * @throws IOException -
   */
  public MagikTypedCheckAssert reportsNoIssues(
      final Path path, final IDefinitionKeeper definitionKeeper)
      throws IllegalArgumentException, IOException {
    this.isNotNull();

    final List<MagikIssue> issues = this.runCheck(path, definitionKeeper);
    if (!issues.isEmpty()) {
      this.failWithMessage(
          "Expected check to report no issues, but gotten <%s> issues", issues.size());
    }

    return this;
  }

  /**
   * Test running the {@link MagikTypedCheck} against code results in a specified number of issues.
   *
   * @param code Code to run {@link MagikTypedCheck} against.
   * @param issueCount Expected issue count.
   * @return Self.
   */
  public MagikTypedCheckAssert reportsIssueCount(
      final String code, final IDefinitionKeeper definitionKeeper, final int issueCount) {
    this.isNotNull();

    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper);
    if (issues.size() != issueCount) {
      this.failWithMessage(
          "Expected check to report <%s> issues, but gotten <%s> issues",
          issueCount, issues.size());
    }

    return this;
  }

  /**
   * Test running the {@link MagikTypedCheck} on path results in a specified number of issues.
   *
   * @param path Path of file to run {@link MagikTypedCheck} against.
   * @param issueCount Expected issue count.
   * @return Self.
   * @throws IllegalArgumentException -
   * @throws IOException -
   */
  public MagikTypedCheckAssert reportsIssueCount(
      final Path path, final IDefinitionKeeper definitionKeeper, final int issueCount)
      throws IllegalArgumentException, IOException {
    this.isNotNull();

    final List<MagikIssue> issues = this.runCheck(path, definitionKeeper);
    if (issues.size() != issueCount) {
      this.failWithMessage(
          "Expected check to report <%s> issues, but gotten <%s> issues",
          issueCount, issues.size());
    }

    return this;
  }

  private List<MagikIssue> runCheck(final String code, final IDefinitionKeeper definitionKeeper)
      throws IllegalArgumentException {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    return this.actual.scanFileForIssues(magikFile);
  }

  private List<MagikIssue> runCheck(
      final Path relativePath, final IDefinitionKeeper definitionKeeper)
      throws IllegalArgumentException, IOException {
    // Ensure proper path.
    final Path currentPath = Path.of(".").toAbsolutePath().getParent();
    final Path fixedPath =
        currentPath.endsWith("magik-checks")
            ? Path.of("..").resolve(relativePath)
            : Path.of(".").resolve(relativePath);

    final URI uri = fixedPath.toUri();
    final String code = Files.readString(fixedPath);
    final MagikTypedFile magikFile = new MagikTypedFile(uri, code, definitionKeeper);
    return this.actual.scanFileForIssues(magikFile);
  }
}
