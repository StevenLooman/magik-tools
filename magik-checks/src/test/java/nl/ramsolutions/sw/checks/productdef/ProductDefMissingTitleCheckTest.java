package nl.ramsolutions.sw.checks.productdef;

import static nl.ramsolutions.sw.checks.productdef.ProductDefCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.ProductDefCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link ProductDefMissingTitleCheck}. */
class ProductDefMissingTitleCheckTest {

  @Test
  void testOk() {
    final ProductDefCheck check = new ProductDefMissingTitleCheck();
    final String code =
        """
        dummy layered_product

        title
          Dummy product
        end
        """;
    assertThat(check).reportsNoIssues(code);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        dummy layered_product
        """,
        """
        dummy layered_product

        title
        end
        """,
        """
        dummy layered_product

        title

        end
        """,
      })
  void testInvalid(final String code) {
    final ProductDefCheck check = new ProductDefMissingTitleCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
