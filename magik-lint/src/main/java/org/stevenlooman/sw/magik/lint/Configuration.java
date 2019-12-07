package org.stevenlooman.sw.magik.lint;

import org.stevenlooman.sw.magik.CheckList;
import org.stevenlooman.sw.magik.MagikCheck;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;

public class Configuration {

  static Logger logger = Logger.getLogger(Configuration.class.getName());

  private Properties properties = new Properties();

  Configuration() {
    logger.fine("Using default configuration");
    setTemplatedChecksDisabled();
    logConfiguration();
  }

  Configuration(Path path) {
    logger.fine("Reading configuration from: " + path);
    setTemplatedChecksDisabled();
    readFileProperties(path);
    logConfiguration();
  }

  private void setTemplatedChecksDisabled() {
    String templatedCheckNames = getTemplatedCheckNames();
    properties.put("disabled", templatedCheckNames);

    // manually: for now, disable sw-method-doc check
    String disabled = (String) properties.get("disabled");
    disabled += ",sw-method-doc";
    properties.put("disabled", disabled);
  }

  private String getTemplatedCheckNames() {
    return CheckList.getTemplatedChecks().stream()
        .map(checkClass -> checkClass.getAnnotation(org.sonar.check.Rule.class))
        .filter(rule -> rule != null)
        .map(rule -> rule.key())
        .map(MagikCheck::toKebabCase)
        .collect(Collectors.joining(","));
  }

  private void readFileProperties(Path path) {
    try {
      InputStream inputStream = new FileInputStream(path.toFile());
      properties.load(inputStream);
    } catch (FileNotFoundException exception) {
      return;
    } catch (IOException exception) {
      System.out.println("Caught error reading properties: " + exception.getMessage());
      exception.printStackTrace();
    }
  }

  @CheckForNull
  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  /**
   * Get property as strings, split by a comma (',').
   * 
   * @param key Key of property
   * @return Value of property split by a comma
   */
  @CheckForNull
  public List<String> getPropertySplit(String key) {
    String line = getProperty(key);
    if (line == null) {
      return null;
    }

    String[] items = line.split(",");
    return Arrays.stream(items).map(String::trim).collect(Collectors.toList());
  }

  /**
   * Get property as int.
   * 
   * @param key Key of property
   * @return Value of property as integer
   */
  @CheckForNull
  public Integer getPropertyInt(String key) {
    String value = getProperty(key);
    if (value == null) {
      return null;
    }

    return Integer.valueOf(value);
  }

  /**
   * Test if property with key exists.
   * 
   * @param key Key of property
   * @return True if it exists, false if not
   */
  public boolean hasProperty(String key) {
    return properties.getProperty(key) != null;
  }

  private void logConfiguration() {
    logger.fine("Configuration:");
    List<?> propertyNames = Collections.list(properties.propertyNames());
    for (Object propertyName : propertyNames) {
      String name = (String)propertyName;
      Object propertyValue = properties.get(name);
      logger.fine(" " + name + ": " + propertyValue);
    }
  }

}
