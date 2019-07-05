package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class ParserErrorCheckTest extends MagikCheckTestBase {

  @Test
  public void testParserError() {
    MagikCheck check = new ParserErrorCheck();
    String code =
        "_block\n" +
        "_endbloc";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

}
