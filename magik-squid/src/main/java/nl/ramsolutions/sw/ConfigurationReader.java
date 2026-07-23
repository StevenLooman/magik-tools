package nl.ramsolutions.sw;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class to determine the location of the properties file to read. */
public final class ConfigurationReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationReader.class);

  private ConfigurationReader() {}

  /**
   * Determine the path of the `magik-lint.properties` file to read.
   *
   * @param overridePath Override path.
   * @return Determined path.
   */
  public static Path determinePath(final Path path, final @Nullable String overridePath) {
    if (overridePath != null && !overridePath.isBlank()) {
      return Path.of(overridePath);
    }

    return ConfigurationLocator.locateConfiguration(path);
  }

  /**
   * Read properties.
   *
   * @param path Path to read properties from.
   * @param overridePath Override path.
   * @return Properties read.
   * @throws IOException -
   */
  public static MagikToolsProperties readProperties(
      final Path path, final @Nullable String overridePath) throws IOException {
    final Path propertiesPath = ConfigurationReader.determinePath(path, overridePath);
    return propertiesPath != null
        ? new MagikToolsProperties(propertiesPath)
        : MagikToolsProperties.DEFAULT_PROPERTIES;
  }

  /**
   * Read properties from a URI, and merges it with the given properties.
   *
   * <p>Uses {@link MagikLintSettings#KEY_OVERRIDE_CONFIG} in case the settings file is overridden.
   *
   * @param path Path to read properties from.
   * @param properties Properties to use as base.
   * @return Properties read, merged with the given properties.
   */
  public static MagikToolsProperties readProperties(
      final Path path, final MagikToolsProperties properties) throws IOException {
    final String overrideConfigFile = new MagikLintSettings(properties).getOverrideConfigFile();
    final Path propertiesPath = ConfigurationReader.determinePath(path, overrideConfigFile);
    if (propertiesPath != null) {
      LOGGER.debug("Reading properties from: {}", propertiesPath);
    }

    // Copy properties, but override all from propertiesPath.
    final MagikToolsProperties fileProperties =
        propertiesPath != null
            ? new MagikToolsProperties(propertiesPath)
            : MagikToolsProperties.DEFAULT_PROPERTIES;

    return MagikToolsProperties.merge(properties, fileProperties);
  }

  /**
   * Read properties from a URI, and merges it with the given properties.
   *
   * <p>Uses {@link MagikLintSettings#KEY_OVERRIDE_CONFIG} in case the settings file is overridden.
   *
   * @param uri URI to read properties from.
   * @param properties Properties to override.
   * @return Properties read, merged with the given properties.
   * @throws IOException -
   */
  public static MagikToolsProperties readProperties(
      final URI uri, final MagikToolsProperties properties) throws IOException {
    final Path path = Path.of(uri);
    return ConfigurationReader.readProperties(path, properties);
  }
}
