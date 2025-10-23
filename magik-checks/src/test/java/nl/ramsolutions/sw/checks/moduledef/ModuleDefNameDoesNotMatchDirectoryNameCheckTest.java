package nl.ramsolutions.sw.checks.moduledef;

import static nl.ramsolutions.sw.checks.moduledef.ModuleDefCheckAssert.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.checks.ModuleDefCheck;
import org.junit.jupiter.api.Test;

/** Test {@link ModuleDefNameDoesNotMatchDirectoryNameCheck}. */
class ModuleDefNameDoesNotMatchDirectoryNameCheckTest {

  @Test
  void testOk() throws IllegalArgumentException, IOException {
    final ModuleDefCheck check = new ModuleDefNameDoesNotMatchDirectoryNameCheck();
    final Path path =
        Path.of("magik-checks/src/test/resources/test_product/modules/test_module/module.def");
    assertThat(check).reportsNoIssues(path);
  }

  @Test
  void testOkIgnored() throws IllegalArgumentException, IOException {
    final ModuleDefNameDoesNotMatchDirectoryNameCheck check =
        new ModuleDefNameDoesNotMatchDirectoryNameCheck();
    check.ignoredDirectoryNames = "magik_sessions,test_module_2";
    final Path path =
        Path.of("magik-checks/src/test/resources/test_product_2/modules/test_module_2/module.def");
    assertThat(check).reportsNoIssues(path);
  }

  @Test
  void testInvalid() throws IllegalArgumentException, IOException {
    final ModuleDefCheck check = new ModuleDefNameDoesNotMatchDirectoryNameCheck();
    final Path path =
        Path.of("magik-checks/src/test/resources/test_product_2/modules/test_module_2/module.def");
    assertThat(check).reportsIssueCount(path, 1);
  }
}
