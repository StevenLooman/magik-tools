package nl.ramsolutions.sw.magik.checks.checks;

import static nl.ramsolutions.sw.magik.checks.checks.MagikCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.api.Test;

/** Test {@link FileNotInLoadListCheck}. */
class FileNotInLoadListCheckTest {

  @Test
  void testNotInLoadList() throws IllegalArgumentException, IOException {
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/test_module/source/not_in_load_list.magik");
    final MagikCheck check = new FileNotInLoadListCheck();
    assertThat(check).reportsIssueCount(path, 1);
  }

  @Test
  void testInLoadList() throws IllegalArgumentException, IOException {
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/test_module/source/in_load_list.magik");
    final MagikCheck check = new FileNotInLoadListCheck();
    assertThat(check).reportsNoIssues(path);
  }
}
