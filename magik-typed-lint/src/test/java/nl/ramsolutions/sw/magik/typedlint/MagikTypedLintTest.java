package nl.ramsolutions.sw.magik.typedlint;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.Issue;
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
}
