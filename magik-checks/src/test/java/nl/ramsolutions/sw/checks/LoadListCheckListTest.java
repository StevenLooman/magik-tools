package nl.ramsolutions.sw.checks;

import static nl.ramsolutions.sw.checks.CheckClassAssert.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Tests for {@link LoadListCheckList}. */
class LoadListCheckListTest {

  @Test
  void testAllChecksHaveValidAdministration() throws IOException {
    for (final Class<? extends LoadListCheck> checkClass : LoadListCheckList.INSTANCE.getChecks()) {
      assertThat(checkClass)
          .hasProperRuleAnnotation()
          .hasMetadata()
          .hasHtmlFile()
          .metadataRuleSpecificationMatchesClassName()
          .metadataSqKeyMatchesClassName();
    }
  }
}
