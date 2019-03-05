package org.stevenlooman.sw.magik.lint;

import javax.annotation.CheckForNull;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

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

  @CheckForNull
  public List<String> getPropertySplit(String key) {
    String line = getProperty(key);
    if (line == null) {
      return null;
    }

    String[] items = line.split(",");
    return Arrays.stream(items).map(String::trim).collect(Collectors.toList());
  }

  @CheckForNull
  public Integer getPropertyInt(String key) {
    String value = getProperty(key);
    if (value == null) {
      return null;
    }

    return Integer.valueOf(value);
  }

  public boolean hasProperty(String key) {
    return properties.contains(key);
  }

}
