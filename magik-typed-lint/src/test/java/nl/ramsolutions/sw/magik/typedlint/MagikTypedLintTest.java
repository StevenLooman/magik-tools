package nl.ramsolutions.sw.magik.typedlint;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.output.NullReporter;
import nl.ramsolutions.sw.checks.output.Reporter;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.junit.jupiter.api.Test;

/** Tests for {@link MagikTypedLint}. */
class MagikTypedLintTest {

  /** {@link Reporter} collecting the reported {@link Issue}s. */
  private static final class CollectingReporter implements Reporter {

    private final List<Issue> issues = new ArrayList<>();

    @Override
    public void reportIssue(final Issue issue) {
      this.issues.add(issue);
    }

    @Override
    public Set<String> reportedSeverities() {
      return Set.of();
    }

    List<Issue> getIssues() {
      return this.issues;
    }
  }

  @Test
  void testIssueLocationIsNormalizedForRelativePath() throws Exception {
    final Path path = Path.of("./src/test/resources/test_product/test_module/source/a.magik");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final CollectingReporter reporter = new CollectingReporter();
    final MagikTypedLint lint =
        new MagikTypedLint(definitionKeeper, MagikToolsProperties.DEFAULT_PROPERTIES, reporter);

    lint.run(List.of(path));

    final Path expectedPath = path.toAbsolutePath().normalize();
    assertThat(reporter.getIssues())
        .isNotEmpty()
        .allSatisfy(issue -> assertThat(issue.location().getPath()).isEqualTo(expectedPath));
  }

  @Test
  void testShowConfigurationWithPath() throws Exception {
    final MagikTypedLint lint = this.createLint();
    final StringWriter writer = new StringWriter();
    final Path path = Path.of("./magik-lint.properties");

    lint.showConfiguration(writer, path);

    assertThat(writer.toString())
        .isEqualTo("Configuration file: " + path.toAbsolutePath().normalize() + "\n\n");
  }

  @Test
  void testShowConfigurationWithoutPath() throws Exception {
    final MagikTypedLint lint = this.createLint();
    final StringWriter writer = new StringWriter();

    lint.showConfiguration(writer, null);

    assertThat(writer.toString()).isEqualTo("Configuration file: (none found, using defaults)\n\n");
  }

  @Test
  void testRunReportsOtherFilesWhenOneFileThrows() throws Exception {
    final Path failingPath = Path.of("failing.magik");
    final Path path = Path.of("./src/test/resources/test_product/test_module/source/a.magik");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final CollectingReporter reporter = new CollectingReporter();
    final MagikTypedLint lint =
        new MagikTypedLint(definitionKeeper, MagikToolsProperties.DEFAULT_PROPERTIES, reporter) {
          @Override
          List<Issue> lintFile(final Path lintedPath) {
            if (lintedPath.equals(failingPath)) {
              throw new IllegalStateException("reasoner failure");
            }

            return super.lintFile(lintedPath);
          }
        };

    lint.run(List.of(failingPath, path));

    final List<Issue> issues = reporter.getIssues();
    assertThat(issues).isNotEmpty();
    final Collection<Path> failedPaths = lint.getFailedPaths();
    assertThat(failedPaths).containsExactly(failingPath);
  }

  @Test
  void testRunSurvivesAStackOverflowInOneFile() throws Exception {
    // A reasoner that recurses without end throws an Error, not an exception.
    final Path failingPath = Path.of("failing.magik");
    final MagikTypedLint lint =
        new MagikTypedLint(
            new DefinitionKeeper(), MagikToolsProperties.DEFAULT_PROPERTIES, new NullReporter()) {
          @Override
          List<Issue> lintFile(final Path path) {
            throw new StackOverflowError();
          }
        };

    lint.run(List.of(failingPath));

    final Collection<Path> failedPaths = lint.getFailedPaths();
    assertThat(failedPaths).containsExactly(failingPath);
  }

  private MagikTypedLint createLint() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    return new MagikTypedLint(
        definitionKeeper, MagikToolsProperties.DEFAULT_PROPERTIES, new NullReporter());
  }
}
