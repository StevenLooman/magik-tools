package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikIssue;

import java.util.List;

public class XPathCheckTest extends MagikCheckTestBase {

  @Test
  public void testMatch() {
    XPathCheck check = new XPathCheck();
    check.xpathQuery = "//RETURN";

    String code =
        "_method a.b\n" +
        "\t_return _self\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testNoMatch() {
    XPathCheck check = new XPathCheck();
    check.xpathQuery = "//IF";

    String code =
        "_method a.b\n" +
        "\t_return _self\n" +
        "_endmethod";
    List<MagikIssue> issues = runCheck(code, check);
    assertThat(issues).isEmpty();
  }

}
