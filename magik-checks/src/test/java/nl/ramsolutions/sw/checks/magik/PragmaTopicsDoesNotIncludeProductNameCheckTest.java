package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;

/** Tests {@link PragmaTopicsDoesNotIncludeProductNameCheck}. */
public class PragmaTopicsDoesNotIncludeProductNameCheckTest {

  @Test
  void testValid() throws IOException {
    final MagikCheck check = new PragmaTopicsDoesNotIncludeProductNameCheck();
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product_2/modules/test_module_2/source/test_exemplar.magik");
    assertThat(check).reportsNoIssues(path);
  }

  @Test
  void testInvalid() throws IOException {
    final MagikCheck check = new PragmaTopicsDoesNotIncludeProductNameCheck();
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/modules/test_module/source/test_exemplar.magik");
    assertThat(check).reportsIssueCount(path, 1);
  }
}
