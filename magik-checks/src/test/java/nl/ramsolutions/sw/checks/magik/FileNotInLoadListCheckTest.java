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

  final Path TEST_PRODUCT_PATH = Path.of("magik-checks/src/test/resources/test_product");
  final Path TEST_MODULE_PATH = TEST_PRODUCT_PATH.resolve("modules/test_module");

  @Test
  void testNotInLoadList() throws IllegalArgumentException, IOException {
    final Path path = TEST_MODULE_PATH.resolve("source/not_in_load_list.magik");
    final MagikCheck check = new FileNotInLoadListCheck();
    assertThat(check).reportsIssueCount(path, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "source/in_load_list.magik",
        "source/in_load_list_2.magik",
        "source/in_load_list_3.magik",
      })
  void testInLoadList(final String filename) throws IllegalArgumentException, IOException {
    final Path path = TEST_MODULE_PATH.resolve(filename);
    final MagikCheck check = new FileNotInLoadListCheck();
    assertThat(check).reportsNoIssues(path);
  }
}
