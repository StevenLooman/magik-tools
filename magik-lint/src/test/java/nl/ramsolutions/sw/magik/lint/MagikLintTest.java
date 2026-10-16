package nl.ramsolutions.sw.magik.lint;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.output.NullReporter;
import org.junit.jupiter.api.Test;

/** Tests for {@link MagikLint}. */
class MagikLintTest {

  @Test
  void testShowConfigurationWithPath() throws Exception {
    final MagikLint lint = this.createLint();
    final StringWriter writer = new StringWriter();
    final Path path = Path.of("./magik-lint.properties");

    lint.showConfiguration(writer, path);

    assertThat(writer.toString())
        .isEqualTo("Configuration file: " + path.toAbsolutePath().normalize() + "\n\n");
  }

  @Test
  void testShowConfigurationWithoutPath() throws Exception {
    final MagikLint lint = this.createLint();
    final StringWriter writer = new StringWriter();

    lint.showConfiguration(writer, null);

    assertThat(writer.toString()).isEqualTo("Configuration file: (none found, using defaults)\n\n");
  }

  @Test
  void testRunLintsOtherFilesWhenOneFileThrows() throws Exception {
    final Path failingPath = Path.of("failing.magik");
    final Path otherPath = Path.of("other.magik");
    final AtomicInteger lintedCount = new AtomicInteger();
    final MagikLint lint =
        new MagikLint(MagikToolsProperties.DEFAULT_PROPERTIES, new NullReporter()) {
          @Override
          List<Issue> lintFile(final Path path) {
            if (path.equals(failingPath)) {
              throw new IllegalStateException("check failure");
            }

            lintedCount.incrementAndGet();
            return List.of();
          }
        };

    lint.run(List.of(failingPath, otherPath));

    final int linted = lintedCount.get();
    assertThat(linted).isEqualTo(1);
    final Collection<Path> failedPaths = lint.getFailedPaths();
    assertThat(failedPaths).containsExactly(failingPath);
  }

  @Test
  void testRunSurvivesAStackOverflowInOneFile() throws Exception {
    // A reasoner that recurses without end throws an Error, not an exception.
    final Path failingPath = Path.of("failing.magik");
    final MagikLint lint =
        new MagikLint(MagikToolsProperties.DEFAULT_PROPERTIES, new NullReporter()) {
          @Override
          List<Issue> lintFile(final Path path) {
            throw new StackOverflowError();
          }
        };

    lint.run(List.of(failingPath));

    final Collection<Path> failedPaths = lint.getFailedPaths();
    assertThat(failedPaths).containsExactly(failingPath);
  }

  private MagikLint createLint() {
    return new MagikLint(MagikToolsProperties.DEFAULT_PROPERTIES, new NullReporter());
  }
}
