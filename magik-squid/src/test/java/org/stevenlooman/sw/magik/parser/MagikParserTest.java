package org.stevenlooman.sw.magik.parser;

import static org.fest.assertions.Assertions.assertThat;
import org.junit.Test;

public class MagikParserTest {

  @Test
  public void testParseIdentifier() {
    assertThat(MagikParser.parseIdentifier("abc")).isEqualTo("abc");
    assertThat(MagikParser.parseIdentifier("ABC")).isEqualTo("abc");
    assertThat(MagikParser.parseIdentifier("abc|def|")).isEqualTo("abcdef");
    assertThat(MagikParser.parseIdentifier("abc| |")).isEqualTo("abc ");
    assertThat(MagikParser.parseIdentifier("abc|def|ghi")).isEqualTo("abcdefghi");
    assertThat(MagikParser.parseIdentifier("abc|DEF|")).isEqualTo("abcDEF");
    assertThat(MagikParser.parseIdentifier("abc|DEF|ghi|JKL|")).isEqualTo("abcDEFghiJKL");
    assertThat(MagikParser.parseIdentifier("abc| |")).isEqualTo("abc ");
    assertThat(MagikParser.parseIdentifier("\\|")).isEqualTo("|");
    assertThat(MagikParser.parseIdentifier("\\|a")).isEqualTo("|a");
  }

}
