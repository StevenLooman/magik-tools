package nl.ramsolutions.sw.magik.lint;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.nio.file.Path;
import nl.ramsolutions.sw.MagikToolsProperties;
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

  private MagikLint createLint() {
    return new MagikLint(MagikToolsProperties.DEFAULT_PROPERTIES, new NullReporter());
  }
}
