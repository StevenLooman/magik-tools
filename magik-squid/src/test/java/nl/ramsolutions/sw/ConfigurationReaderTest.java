package nl.ramsolutions.sw;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link ConfigurationReader}. */
class ConfigurationReaderTest {

  @TempDir private Path tempDir;

  @BeforeEach
  void resetConfigurationLocatorCache() {
    ConfigurationLocator.resetCache();
  }

  @Test
  void testDeterminePathUsesOverridePath() throws IOException {
    final Path sourcePath = this.createProductDirWithConfiguration();
    final Path overridePath = this.tempDir.resolve("override-magik-lint.properties");
    Files.createFile(overridePath);

    final Path path = ConfigurationReader.determinePath(sourcePath, overridePath.toString());

    assertThat(path).isEqualTo(overridePath);
  }

  @Test
  void testDeterminePathLocatesConfigurationInProductDir() throws IOException {
    final Path sourcePath = this.createProductDirWithConfiguration();
    final Path configurationPath =
        sourcePath.getParent().resolve(ConfigurationLocator.MAGIK_LINT_RC_FILENAME);

    final Path path = ConfigurationReader.determinePath(sourcePath, null);

    assertThat(path).isEqualTo(configurationPath);
  }

  @Test
  void testDeterminePathIgnoresBlankOverridePath() throws IOException {
    final Path sourcePath = this.createProductDirWithConfiguration();
    final Path configurationPath =
        sourcePath.getParent().resolve(ConfigurationLocator.MAGIK_LINT_RC_FILENAME);

    final Path path = ConfigurationReader.determinePath(sourcePath, " ");

    assertThat(path).isEqualTo(configurationPath);
  }

  private Path createProductDirWithConfiguration() throws IOException {
    final Path productDir = this.tempDir.resolve("test_product");
    Files.createDirectories(productDir);
    Files.createFile(productDir.resolve("product.def"));
    Files.createFile(productDir.resolve(ConfigurationLocator.MAGIK_LINT_RC_FILENAME));

    final Path sourcePath = productDir.resolve("a.magik");
    Files.createFile(sourcePath);
    return sourcePath;
  }
}
