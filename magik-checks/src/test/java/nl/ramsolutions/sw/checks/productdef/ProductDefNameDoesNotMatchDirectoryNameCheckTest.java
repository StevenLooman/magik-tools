package nl.ramsolutions.sw.checks.productdef;

import static nl.ramsolutions.sw.checks.productdef.ProductDefCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.checks.ProductDefCheck;
import org.junit.jupiter.api.Test;

/** Test {@link ProductDefNameDoesNotMatchDirectoryNameCheck}. */
class ProductDefNameDoesNotMatchDirectoryNameCheckTest {

  @Test
  void testOk() throws IllegalArgumentException, IOException {
    final ProductDefCheck check = new ProductDefNameDoesNotMatchDirectoryNameCheck();
    final Path path = Path.of("magik-checks/src/test/resources/test_product/product.def");
    assertThat(check).reportsNoIssues(path);
  }

  @Test
  void testOkIgnored() throws IllegalArgumentException, IOException {
    final ProductDefNameDoesNotMatchDirectoryNameCheck check =
        new ProductDefNameDoesNotMatchDirectoryNameCheck();
    check.ignoredDirectoryNames = "config,test_product_2";
    final Path path = Path.of("magik-checks/src/test/resources/test_product_2/product.def");
    assertThat(check).reportsNoIssues(path);
  }

  @Test
  void testInvalid() throws IllegalArgumentException, IOException {
    final ProductDefCheck check = new ProductDefNameDoesNotMatchDirectoryNameCheck();
    final Path path = Path.of("magik-checks/src/test/resources/test_product_2/product.def");
    assertThat(check).reportsIssueCount(path, 1);
  }
}
