package nl.ramsolutions.sw;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/** Class to determine the location of the properties file to read. */
public final class ConfigurationReader {

  private ConfigurationReader() {}

  /**
   * Determine the path of the `magik-lint.properties` file to read.
   *
   * @param overridePath
   * @return
   */
  public static Path determinePath(final Path path, final @Nullable String overridePath) {
    if (overridePath != null && !overridePath.isBlank()) {
      return Path.of(overridePath);
    }

    return ConfigurationLocator.locateConfiguration(path);
  }

  public static MagikToolsProperties readProperties(
      final Path path, final @Nullable String overridePath) throws IOException {
    final Path propertiesPath = ConfigurationReader.determinePath(path, overridePath);
    return propertiesPath != null
        ? new MagikToolsProperties(propertiesPath)
        : MagikToolsProperties.DEFAULT_PROPERTIES;
  }

  /**
   * Read properties.
   *
   * <p>Uses {@link nl.ramsolutions.sw.magik.lint.MagikLint.KEY_OVERRIDE_CONFIG} in case the
   * settings file is overridden.
   */
  public static MagikToolsProperties readProperties(
      final Path path, final MagikToolsProperties properties) throws IOException {
    // final String overridePath = properties.getPropertyString(MagikLint.KEY_OVERRIDE_CONFIG);
    final String overrideConfigFile = properties.getPropertyString("magik.lint.overrideConfigFile");
    final Path propertiesPath = ConfigurationReader.determinePath(path, overrideConfigFile);

    // Copy properties, but override all from propertiesPath.
    final MagikToolsProperties fileProperties =
        propertiesPath != null
            ? new MagikToolsProperties(propertiesPath)
            : MagikToolsProperties.DEFAULT_PROPERTIES;

    return MagikToolsProperties.merge(properties, fileProperties);
  }

  public static MagikToolsProperties readProperties(
      final URI uri, final MagikToolsProperties properties) throws IOException {
    final Path path = Path.of(uri);
    return ConfigurationReader.readProperties(path, properties);
  }
}
