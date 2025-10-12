package nl.ramsolutions.sw.checks.productdef;

import static nl.ramsolutions.sw.checks.productdef.ProductDefCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.checks.ProductDefCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link ProductDefMissingDescriptionCheck}. */
class ProductDefMissingDescriptionCheckTest {

  @Test
  void testOk() {
    final ProductDefCheck check = new ProductDefMissingDescriptionCheck();
    final String code =
        """
        dummy layered_product

        description
          Dummy description
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

        description
        end
        """,
        """
        dummy layered_product

        description

        end
        """,
      })
  void testInvalid(final String code) {
    final ProductDefCheck check = new ProductDefMissingDescriptionCheck();
    assertThat(check).reportsIssueCount(code, 1);
  }
}
