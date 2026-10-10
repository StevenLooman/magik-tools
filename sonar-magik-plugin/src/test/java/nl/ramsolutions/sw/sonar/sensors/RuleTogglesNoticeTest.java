package nl.ramsolutions.sw.sonar.sensors;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link RuleTogglesNotice}. */
class RuleTogglesNoticeTest {

  private static final String PROPERTIES = "magik-lint.properties";

  private Path writeProperties(final Path directory, final String content) throws IOException {
    Files.createDirectories(directory);
    final Path file = directory.resolve(PROPERTIES);
    Files.writeString(file, content);
    return file.toAbsolutePath().normalize();
  }

  @Test
  void testDisabledEntryInTheProductDirIsFound(@TempDir final Path baseDir) throws IOException {
    final Path file = this.writeProperties(baseDir, "disabled=one-check\n");
    Files.writeString(baseDir.resolve("product.def"), "");
    final Path scanned = Files.createDirectories(baseDir.resolve("a").resolve("b"));

    final List<Path> found = RuleTogglesNotice.findFilesWithToggles(List.of(scanned));

    assertThat(found).containsExactly(file);
  }

  @Test
  void testEnabledEntryIsFound(@TempDir final Path baseDir) throws IOException {
    final Path file = this.writeProperties(baseDir, "enabled=one-check\n");

    final List<Path> found = RuleTogglesNotice.findFilesWithToggles(List.of(baseDir));

    assertThat(found).containsExactly(file);
  }

  @Test
  void testRuleSettingIsFound(@TempDir final Path baseDir) throws IOException {
    final Path file = this.writeProperties(baseDir, "method-complexity.maximum-complexity=20\n");

    final List<Path> found = RuleTogglesNotice.findFilesWithToggles(List.of(baseDir));

    assertThat(found).containsExactly(file);
  }

  @Test
  void testIgnoredPathsAreFound(@TempDir final Path baseDir) throws IOException {
    final Path file = this.writeProperties(baseDir, "ignore=generated/\n");

    final List<Path> found = RuleTogglesNotice.findFilesWithToggles(List.of(baseDir));

    assertThat(found).containsExactly(file);
  }

  @Test
  void testFileWithOnlyToolSettingsIsNotFound(@TempDir final Path baseDir) throws IOException {
    this.writeProperties(
        baseDir, "magik.formatting.indentStrategy=tabs\nmagik.typing.typeDatabasePaths=a.jsonl\n");

    final List<Path> found = RuleTogglesNotice.findFilesWithToggles(List.of(baseDir));

    assertThat(found).isEmpty();
  }

  @Test
  void testFileIsReportedOnceForManyDirectories(@TempDir final Path baseDir) throws IOException {
    final Path file = this.writeProperties(baseDir, "disabled=one-check\n");
    final Path dirA = Files.createDirectories(baseDir.resolve("a"));
    final Path dirB = Files.createDirectories(baseDir.resolve("b"));

    final List<Path> found = RuleTogglesNotice.findFilesWithToggles(List.of(dirA, dirB, baseDir));

    assertThat(found).containsExactly(file);
  }

  @Test
  void testPerDirectoryFilesAreEachFound(@TempDir final Path baseDir) throws IOException {
    final Path fileA = this.writeProperties(baseDir.resolve("a"), "disabled=one-check\n");
    final Path fileB = this.writeProperties(baseDir.resolve("b"), "enabled=other-check\n");

    final List<Path> found =
        RuleTogglesNotice.findFilesWithToggles(List.of(baseDir.resolve("a"), baseDir.resolve("b")));

    assertThat(found).containsExactly(fileA, fileB);
  }

  @Test
  void testNoFileMeansNothingFound(@TempDir final Path baseDir) {
    final List<Path> found = RuleTogglesNotice.findFilesWithToggles(List.of(baseDir));

    assertThat(found).isEmpty();
  }

  @Test
  void testMessageNamesTheFileAndTheProfile() {
    final Path file = Path.of("/p/magik-lint.properties");

    final String message = RuleTogglesNotice.describe(List.of(file));

    // The message prints the path with the separators of the platform.
    assertThat(message)
        .isEqualTo(
            "The rule toggles (enabled, disabled), rule settings and ignored paths in "
                + file
                + " are not used under SonarQube; the quality profile decides which rules run"
                + " and how they are set, and sonar.exclusions which files are skipped");
  }

  @Test
  void testMessageCountsTheOtherFiles() {
    final Path fileA = Path.of("/p/a/magik-lint.properties");
    final Path fileB = Path.of("/p/b/magik-lint.properties");

    final String message = RuleTogglesNotice.describe(List.of(fileA, fileB));

    assertThat(message).contains(fileA + " and 1 more are not used");
  }
}
