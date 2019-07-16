package org.stevenlooman.sw.magik.lint;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;

public class Configuration {

  private Properties properties = new Properties();

  Configuration() {
    properties.put("disabled", "comment-regular-expression,xpath");
  }

  Configuration(Path path) {
    this();
    readFileProperties(path);
  }

  void readFileProperties(Path path) {
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
   * @param key Key of property
   * @return True if it exists, false if not
   */
  public boolean hasProperty(String key) {
    return properties.getProperty(key) != null;
  }

}
