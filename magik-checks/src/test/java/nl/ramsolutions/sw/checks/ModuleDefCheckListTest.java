package nl.ramsolutions.sw.checks;

import static nl.ramsolutions.sw.checks.CheckClassAssert.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Tests for {@link ModuleDefCheckList}. */
class ModuleDefCheckListTest {

  @Test
  void testAllChecksHaveValidAdministration() throws IOException {
    for (final Class<? extends ModuleDefCheck> checkClass :
        ModuleDefCheckList.INSTANCE.getChecks()) {
      assertThat(checkClass)
          .hasProperRuleAnnotation()
          .hasMetadata()
          .hasHtmlFile()
          .metadataRuleSpecificationMatchesClassName()
          .metadataSqKeyMatchesClassName();
    }
  }
}
