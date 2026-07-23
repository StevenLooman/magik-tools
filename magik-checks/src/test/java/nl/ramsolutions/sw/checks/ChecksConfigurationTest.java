package nl.ramsolutions.sw.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.MagikFile;
import org.junit.jupiter.api.Test;

/** Tests for {@link ChecksConfiguration}. */
class ChecksConfigurationTest {

  private static final URI FILE_URI = URI.create("file:///product/module/source/a.magik");

  private OpenedFile createOpenedFile(final String ignores) {
    final MagikToolsProperties properties =
        ignores != null
            ? new MagikToolsProperties(Map.of("ignore", ignores))
            : MagikToolsProperties.DEFAULT_PROPERTIES;
    return new MagikFile(properties, FILE_URI, "_method a.b\n_endmethod\n$\n");
  }

  @Test
  void testNoIgnoresConfigured() {
    final OpenedFile openedFile = this.createOpenedFile(null);

    assertThat(ChecksConfiguration.isFileIgnored(openedFile)).isFalse();
  }

  @Test
  void testEmptyIgnore() {
    final OpenedFile openedFile = this.createOpenedFile("");

    assertThat(ChecksConfiguration.isFileIgnored(openedFile)).isFalse();
  }

  @Test
  void testMatchingIgnore() {
    final OpenedFile openedFile = this.createOpenedFile("glob:**/a.magik");

    assertThat(ChecksConfiguration.isFileIgnored(openedFile)).isTrue();
  }

  @Test
  void testNonMatchingIgnore() {
    final OpenedFile openedFile = this.createOpenedFile("glob:**/b.magik");

    assertThat(ChecksConfiguration.isFileIgnored(openedFile)).isFalse();
  }

  @Test
  void testMatchingIgnoreOnDirectory() {
    final OpenedFile openedFile = this.createOpenedFile("glob:**/module/**");

    assertThat(ChecksConfiguration.isFileIgnored(openedFile)).isTrue();
  }

  @Test
  void testMultipleIgnoresWithOneMatching() {
    final OpenedFile openedFile = this.createOpenedFile("glob:**/b.magik,glob:**/a.magik");

    assertThat(ChecksConfiguration.isFileIgnored(openedFile)).isTrue();
  }

  @Test
  void testMultipleIgnoresWithNoneMatching() {
    final OpenedFile openedFile = this.createOpenedFile("glob:**/b.magik,glob:**/c.magik");

    assertThat(ChecksConfiguration.isFileIgnored(openedFile)).isFalse();
  }

  @Test
  void testIgnoreWithoutSyntaxPrefixIsSkipped() {
    final OpenedFile openedFile = this.createOpenedFile("**/a.magik");

    assertThat(ChecksConfiguration.isFileIgnored(openedFile)).isFalse();
  }

  @Test
  void testIgnoreWithUnknownSyntaxIsSkipped() {
    final OpenedFile openedFile = this.createOpenedFile("wildcard:**/a.magik");

    assertThat(ChecksConfiguration.isFileIgnored(openedFile)).isFalse();
  }

  @Test
  void testIgnoreWithInvalidRegexIsSkipped() {
    final OpenedFile openedFile = this.createOpenedFile("regex:[");

    assertThat(ChecksConfiguration.isFileIgnored(openedFile)).isFalse();
  }

  @Test
  void testInvalidIgnoreDoesNotDisableValidIgnore() {
    final OpenedFile openedFile = this.createOpenedFile("**/a.magik,glob:**/a.magik");

    assertThat(ChecksConfiguration.isFileIgnored(openedFile)).isTrue();
  }
}
