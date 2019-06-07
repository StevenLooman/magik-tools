package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;

public class MagikCheckTest {

  @Test
  public void testCheckKey() {
    MagikCheck check = new TrailingWhitespaceCheck();
    assertThat(check.getCheckKey()).isEqualTo("TrailingWhitespace");
  }

  @Test
  public void testCheckKeyKebabCase() {
    MagikCheck check = new TrailingWhitespaceCheck();
    assertThat(check.getCheckKeyKebabCase()).isEqualTo("trailing-whitespace");
  }

}
