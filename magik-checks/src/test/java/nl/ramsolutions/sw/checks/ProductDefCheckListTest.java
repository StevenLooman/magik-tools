package nl.ramsolutions.sw.checks;

import static nl.ramsolutions.sw.checks.CheckClassAssert.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Tests for {@link ProductDefCheckList}. */
class ProductDefCheckListTest {

  @Test
  void testAllChecksHaveValidAdministration() throws IOException {
    for (final Class<? extends ProductDefCheck> checkClass :
        ProductDefCheckList.INSTANCE.getChecks()) {
      assertThat(checkClass)
          .hasProperRuleAnnotation()
          .hasMetadata()
          .hasHtmlFile()
          .metadataRuleSpecificationMatchesClassName()
          .metadataSqKeyMatchesClassName();
    }
  }
}
