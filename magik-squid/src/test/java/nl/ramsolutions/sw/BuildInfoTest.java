package nl.ramsolutions.sw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** The build bakes its version and commit into a resource every jar carries. */
class BuildInfoTest {

  @Test
  void readsTheVersionTheBuildBakedIn() {
    final BuildInfo info = BuildInfo.getCurrent();

    final String version = info.version();

    assertThat(version).isNotEqualTo(BuildInfo.UNKNOWN_VERSION).isNotBlank();
  }

  @Test
  void resourceIsGeneratedByTheBuild() throws IOException {
    try (InputStream stream = BuildInfo.class.getResourceAsStream(BuildInfo.RESOURCE)) {
      assertThat(stream).isNotNull();
    }
  }

  @Test
  void recordsTheCommitOfAGitCheckoutUnlessACleanTag() throws IOException, InterruptedException {
    final Path workingDirectory = Path.of("");
    final Path start = workingDirectory.toAbsolutePath();
    final boolean gitCheckout = this.isInGitCheckout(start);
    assumeTrue(gitCheckout, "not built from a git checkout");
    final boolean cleanTag = this.isAtCleanTag(start);
    final BuildInfo info = BuildInfo.getCurrent();

    final String commit = info.commit();
    if (cleanTag) {
      assertThat(commit).isNull();
    } else {
      assertThat(commit).isNotNull();
    }
  }

  @Test
  void readsTheCommitWhenRecorded() {
    final Properties properties = this.createProperties("version=1.0\ncommit=0123456789ab\n");
    final BuildInfo info = BuildInfo.createFromProperties(properties);

    final String commit = info.commit();
    assertThat(commit).isEqualTo("0123456789ab");
  }

  @Test
  void readsNoCommitWhenBuiltWithoutGit() {
    final Properties properties = this.createProperties("version=1.0\n");
    final BuildInfo info = BuildInfo.createFromProperties(properties);

    final String commit = info.commit();
    assertThat(commit).isNull();
  }

  @Test
  void readsAnUnknownVersionWithoutTheResource() {
    final Properties properties = this.createProperties("");
    final BuildInfo info = BuildInfo.createFromProperties(properties);

    final String version = info.version();
    assertThat(version).isEqualTo(BuildInfo.UNKNOWN_VERSION);
  }

  @Test
  void describesTheVersionWithTheCommit() {
    final BuildInfo info = new BuildInfo("1.0", "0123456789ab");

    final String description = info.describeVersion();
    assertThat(description).isEqualTo("1.0 (commit 0123456789ab)");
  }

  @Test
  void describesTheVersionAloneWithoutACommit() {
    final BuildInfo info = new BuildInfo("1.0", null);

    final String description = info.describeVersion();
    assertThat(description).isEqualTo("1.0");
  }

  private boolean isInGitCheckout(final Path start) {
    for (Path directory = start; directory != null; directory = directory.getParent()) {
      final Path git = directory.resolve(".git");
      if (Files.exists(git)) {
        return true;
      }
    }
    return false;
  }

  /** Whether HEAD is exactly at a tag with no tracked changes, as the build decides it. */
  private boolean isAtCleanTag(final Path directory) throws IOException, InterruptedException {
    final GitResult describe =
        this.runGitCommand(directory, "describe", "--exact-match", "--tags", "HEAD");
    final GitResult status =
        this.runGitCommand(directory, "status", "--porcelain", "--untracked-files=no");
    final String changes = status.output();
    final boolean dirty = status.exitCode() == 0 && !changes.isEmpty();
    return describe.exitCode() == 0 && !dirty;
  }

  private GitResult runGitCommand(final Path directory, final String... arguments)
      throws IOException, InterruptedException {
    final List<String> command = new ArrayList<>();
    command.add("git");
    Collections.addAll(command, arguments);
    final ProcessBuilder builder = new ProcessBuilder(command);
    final File workingDirectory = directory.toFile();
    builder.directory(workingDirectory);
    builder.redirectError(ProcessBuilder.Redirect.DISCARD);
    final Process process = builder.start();
    try (InputStream stream = process.getInputStream()) {
      final byte[] bytes = stream.readAllBytes();
      final String output = new String(bytes, StandardCharsets.UTF_8);
      final int exitCode = process.waitFor();
      final String trimmed = output.strip();
      return new GitResult(exitCode, trimmed);
    }
  }

  private record GitResult(int exitCode, String output) {}

  private Properties createProperties(final String text) {
    final Properties properties = new Properties();
    try {
      final StringReader reader = new StringReader(text);
      properties.load(reader);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
    return properties;
  }
}
