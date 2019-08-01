package org.stevenlooman.sw.magik.parser;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileCharsetDeterminerTest {

  @Test
  public void testDetermineCharsetOk() {
    Path path = Paths.get("src/test/resources/tests/parser/determine_encoding_1.magik");
    if (!path.toFile().exists()) {
      // For vscode
      path = Paths.get("magik-squid/src/test/resources/tests/parser/determine_encoding_1.magik");
    }
    Charset defaultCharset = Charset.forName("UTF-8");
    Charset result = FileCharsetDeterminer.determineCharset(path, defaultCharset);

    assertThat(result).isEqualTo(Charset.forName("ISO-8859-1"));
  }

  @Test
  public void testDetermineCharsetMissingFile() {
    Path path = Paths.get("non-existant.magik");
    Charset defaultCharset = Charset.forName("UTF-8");
    Charset result = FileCharsetDeterminer.determineCharset(path, defaultCharset);

    assertThat(result).isEqualTo(defaultCharset);
  }
}
