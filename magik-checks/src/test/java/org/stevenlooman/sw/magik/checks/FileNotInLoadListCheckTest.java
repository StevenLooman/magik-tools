package org.stevenlooman.sw.magik.checks;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileNotInLoadListCheckTest extends MagikCheckTestBase {

  @Test
  public void testNotInLoadList() {
    MagikCheck check = new FileNotInLoadListCheck();
    Path path = Paths.get("magik-checks/src/test/resources/test_product/test_module/source/not_in_load_list.magik");
    List<MagikIssue> issues = runCheck(path, check);
    assertThat(issues).isNotEmpty();
  }

  @Test
  public void testInLoadList() {
    MagikCheck check = new FileNotInLoadListCheck();
    Path path = Paths.get("magik-checks/src/test/resources/test_product/test_module/source/in_load_list.magik");
    List<MagikIssue> issues = runCheck(path, check);
    assertThat(issues).isEmpty();
  }

}
