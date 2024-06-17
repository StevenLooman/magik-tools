package nl.ramsolutions.sw;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Magik-tools properties.
 *
 * <p>Note that this is currently used in (at least) three ways: - Settings for
 * magik-language-server settings - Settings for magik-lint from command line - Settings for
 * `magik-lint.properties` files for a given {@link MagikFile}
 *
 * <p>These are separate code-paths, but given the shared used of {@link MagikToolsProperties} the
 * separation can be confusing.
 *
 * <p>The helper classes {@link ConfigurationLocator} and {@link ConfigurationReader} are used to
 * locate and read the settings.
 */
public class MagikToolsProperties {

  public static final MagikToolsProperties DEFAULT_PROPERTIES = new MagikToolsProperties(Map.of());
  public static final String LIST_SEPARATOR = ",";

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikToolsProperties.class);

  private final Properties properties = new Properties();

  public MagikToolsProperties() {}

  public MagikToolsProperties(final Map<String, String> properties) {
    this.properties.putAll(properties);
  }

  public MagikToolsProperties(final Path path) throws IOException {
    LOGGER.debug("Reading configuration from: {}", path.toAbsolutePath());
    try (final FileInputStream inputStream = new FileInputStream(path.toFile())) {
      this.properties.load(inputStream);
    }
  }

  public void clear() {
    this.properties.clear();
  }

  public void reset() {
    this.clear();
    this.putAll(MagikToolsProperties.DEFAULT_PROPERTIES);
  }

  public void putAll(final Properties newProperties) {
    this.properties.putAll(newProperties);
  }

  public void putAll(final MagikToolsProperties newProperties) {
    this.properties.putAll(newProperties.properties);
  }

  /**
   * Set property.
   *
   * @param key Key of property.
   * @param value Value of property.
   */
  public void setProperty(final String key, @Nullable final String value) {
    if (value == null) {
      this.properties.remove(key);
    }

    this.properties.setProperty(key, value);
  }

  /**
   * Set property.
   *
   * @param key Key of property.
   * @param value Value of property.
   */
  public void setProperty(final String key, @Nullable final Integer value) {
    if (value == null) {
      this.properties.remove(key);
    } else {
      final String valueStr = Integer.toString(value);
      this.properties.setProperty(key, valueStr);
    }
  }

  /**
   * Set property.
   *
   * @param key Key of property.
   * @param value Value of property.
   */
  public void setProperty(final String key, @Nullable final Long value) {
    if (value == null) {
      this.properties.remove(key);
    } else {
      final String valueStr = Long.toString(value);
      this.properties.setProperty(key, valueStr);
    }
  }

  /**
   * Set property.
   *
   * @param key Key of property.
   * @param value Value of property.
   */
  public void setProperty(final String key, @Nullable final Boolean value) {
    if (value == null) {
      this.properties.remove(key);
    } else {
      final String valueStr = Boolean.toString(value);
      this.properties.setProperty(key, valueStr);
    }
  }

  /**
   * Get property.
   *
   * @param key Key of property.
   * @return Value of property.
   */
  @CheckForNull
  public String getPropertyString(final String key) {
    return this.properties.getProperty(key);
  }

  /**
   * Get property.
   *
   * @param key Key of property.
   * @param defaultValue Default vaule.
   * @return Value of property.
   */
  public String getPropertyString(final String key, final String defaultValue) {
    return this.properties.getProperty(key, defaultValue);
  }

  /**
   * Get property, converted to a {@link Boolean}.
   *
   * @param key Key of property.
   * @return Value of property.
   */
  @CheckForNull
  public Boolean getPropertyBoolean(final String key) {
    final String value = this.getPropertyString(key);
    if (value == null) {
      return null;
    }

    return Boolean.valueOf(value);
  }

  /**
   * Get property, converted to a {@link Boolean}.
   *
   * @param key Key of property.
   * @param defaultValue Default value.
   * @return Value of property.
   */
  public boolean getPropertyBoolean(final String key, final boolean defaultValue) {
    final String value = this.getPropertyString(key);
    if (value == null) {
      return defaultValue;
    }

    return Boolean.valueOf(value);
  }

  /**
   * Get property, converted to an {@link Integer}.
   *
   * @param key Key of property.
   * @return Value of property.
   */
  @CheckForNull
  public Integer getPropertyInteger(final String key) {
    final String value = this.getPropertyString(key);
    if (value == null) {
      return null;
    }

    return Integer.valueOf(value);
  }

  /**
   * Get property, converted to an {@link Integer}.
   *
   * @param key Key of property.
   * @return Value of property.
   */
  public int getPropertyInteger(final String key, final int defaultValue) {
    final String value = this.getPropertyString(key);
    if (value == null) {
      return defaultValue;
    }

    return Integer.valueOf(value);
  }

  /**
   * Get property, converted to a {@link Long}.
   *
   * @param key Key of property.
   * @return Value of property.
   */
  @CheckForNull
  public Long getPropertyLong(final String key) {
    final String value = this.getPropertyString(key);
    if (value == null) {
      return null;
    }

    return Long.valueOf(value);
  }

  @CheckForNull
  public Path getPropertyPath(final String key) {
    final String value = this.getPropertyString(key);
    if (value == null) {
      return null;
    }

    return Path.of(value);
  }

  public boolean hasProperty(final String key) {
    return this.getPropertyString(key) != null;
  }

  /**
   * Get a property value as a list. Items are separated by {@link
   * MagikToolsProperties.LIST_SEPARATOR}.
   *
   * @param key Key of the property.
   * @return List of values.
   */
  public List<String> getPropertyList(final String key) {
    final String value = this.getPropertyString(key);
    if (value == null || value.isBlank()) {
      return Collections.emptyList();
    }

    final String[] values = value.split(MagikToolsProperties.LIST_SEPARATOR);
    return Arrays.stream(values).map(String::trim).toList();
  }

  /**
   * Merge two sets of properties.
   *
   * <p>In case of duplicate keys, the values of `properties2` win.
   *
   * @param properties1 Properties 1.
   * @param properties2 Properties 2.
   * @return Merged properties.
   */
  public static MagikToolsProperties merge(
      final MagikToolsProperties properties1, final MagikToolsProperties properties2) {
    final MagikToolsProperties result = new MagikToolsProperties();
    properties1.properties.forEach(result.properties::put);
    properties2.properties.forEach(result.properties::put);
    return result;
  }
}
