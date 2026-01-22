package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link FileNotInLoadListCheck}. */
class FileNotInLoadListCheckTest {

  @Test
  void testNotInLoadList() throws IllegalArgumentException, IOException {
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/modules/test_module/source/not_in_load_list.magik");
    final MagikCheck check = new FileNotInLoadListCheck();
    assertThat(check).reportsIssueCount(path, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "in_load_list.magik",
        "also_in_load_list.magik",
        "this_is_also_in_load_list.magik"
      })
  void testInLoadList(final String pathString) throws IllegalArgumentException, IOException {
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/modules/test_module/source", pathString);
    final MagikCheck check = new FileNotInLoadListCheck();
    assertThat(check).reportsNoIssues(path);
  }
}
