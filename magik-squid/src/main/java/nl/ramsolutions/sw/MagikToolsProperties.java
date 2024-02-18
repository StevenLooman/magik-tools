package nl.ramsolutions.sw;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Magik-tools properties. */
public class MagikToolsProperties {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikToolsProperties.class);
  private static final String DEFAULT_PROPERTIES_FILENAME = "magik-tools-defaults.properties";
  private static final String LIST_SEPARATOR = ",";

  private final Properties properties = new Properties();

  public MagikToolsProperties() throws IOException {
    this(
        MagikToolsProperties.class
            .getClassLoader()
            .getResourceAsStream(DEFAULT_PROPERTIES_FILENAME));
    LOGGER.debug("Read default configuration from: {}", DEFAULT_PROPERTIES_FILENAME);
  }

  public MagikToolsProperties(final Path path) throws IOException {
    this(new FileInputStream(path.toFile()));
    LOGGER.debug("Read configuration from: {}", path.toAbsolutePath());
  }

  private MagikToolsProperties(final InputStream stream) throws IOException {
    this.properties.load(stream);
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
    if (value == null) {
      return Collections.emptyList();
    }

    final String[] values = value.split(MagikToolsProperties.LIST_SEPARATOR);
    return Arrays.stream(values).map(String::trim).collect(Collectors.toList());
  }
}
