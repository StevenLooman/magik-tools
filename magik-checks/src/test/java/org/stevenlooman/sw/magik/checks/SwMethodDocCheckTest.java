package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class SwMethodDocCheckTest extends MagikCheckTestBase {

  @Test
  public void testSwMethodDoc() {
    MagikCheck check = new SwMethodDocCheck();
    String code =
      "_method a.b(param1, param2?)\n" +
      "  ## This is an example method. PARAM1 and PARAM2? are used.\n" +
      "  ## Some more doc.\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

  @Test
  public void testSwMethodDocMssing() {
    MagikCheck check = new SwMethodDocCheck();
    String code =
      "_method a.b(param1, param2)\n" +
      "  ## This is an example method.\n" +
      "  ## Some more doc.\n" +
      "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).hasSize(2);
  }

}