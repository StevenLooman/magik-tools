package nl.ramsolutions.sw.checks.output;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Tests for {@link ReporterRegistry}. */
class ReporterRegistryTest {

  @Test
  void testGetFormatsContainsRegisteredFormat() {
    ReporterRegistry.register(
        "test-format-a", (final var properties, final var context) -> new NullReporter());

    assertThat(ReporterRegistry.getFormats()).contains("test-format-a");
  }

  @Test
  void testGetFormatsNormalizesRegisteredFormat() {
    ReporterRegistry.register(
        " Test-Format-B ", (final var properties, final var context) -> new NullReporter());

    assertThat(ReporterRegistry.getFormats()).contains("test-format-b");
  }

  @Test
  void testHasFormatForRegisteredFormat() {
    ReporterRegistry.register(
        "test-format-c", (final var properties, final var context) -> new NullReporter());

    assertThat(ReporterRegistry.hasFormat("test-format-c")).isTrue();
    assertThat(ReporterRegistry.hasFormat(" Test-Format-C ")).isTrue();
  }

  @Test
  void testHasFormatForUnknownFormat() {
    assertThat(ReporterRegistry.hasFormat("test-format-unknown")).isFalse();
  }
}
