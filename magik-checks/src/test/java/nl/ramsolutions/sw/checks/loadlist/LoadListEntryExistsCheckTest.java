package nl.ramsolutions.sw.checks.loadlist;

import static nl.ramsolutions.sw.checks.loadlist.LoadListCheckAssert.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.checks.LoadListCheck;
import org.junit.jupiter.api.Test;

/** Test {@link LoadListEntryExistsCheck}. */
class LoadListEntryExistsCheckTest {

  @Test
  void testValidEntriesOnly() throws IOException {
    final LoadListCheck check = new LoadListEntryExistsCheck();
    final Path path =
        Path.of("magik-checks/src/test/resources/test_load_list_check/valid/load_list.txt");
    assertThat(check).reportsNoIssues(path);
  }

  @Test
  void testMissingFile() throws IOException {
    final LoadListCheck check = new LoadListEntryExistsCheck();
    final Path path =
        Path.of("magik-checks/src/test/resources/test_load_list_check/missing_file/load_list.txt");
    assertThat(check).reportsIssueCount(path, 1);
  }

  @Test
  void testMissingDirectory() throws IOException {
    final LoadListCheck check = new LoadListEntryExistsCheck();
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_load_list_check/missing_directory/load_list.txt");
    assertThat(check).reportsIssueCount(path, 1);
  }

  @Test
  void testDirectoryWithoutLoadList() throws IOException {
    final LoadListCheck check = new LoadListEntryExistsCheck();
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_load_list_check/directory_no_loadlist/load_list.txt");
    assertThat(check).reportsIssueCount(path, 1);
  }

  @Test
  void testMixedScenario() throws IOException {
    final LoadListCheck check = new LoadListEntryExistsCheck();
    final Path path =
        Path.of("magik-checks/src/test/resources/test_load_list_check/mixed/load_list.txt");
    assertThat(check).reportsIssueCount(path, 2);
  }

  @Test
  void testExistingTestProduct() throws IOException {
    final LoadListCheck check = new LoadListEntryExistsCheck();
    final Path path =
        Path.of("magik-checks/src/test/resources/test_product/modules/test_module/load_list.txt");
    assertThat(check).reportsNoIssues(path);
  }

  @Test
  void testExistingTestProduct2() throws IOException {
    final LoadListCheck check = new LoadListEntryExistsCheck();
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product_2/modules/test_module_2/load_list.txt");
    assertThat(check).reportsNoIssues(path);
  }
}
