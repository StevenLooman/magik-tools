package nl.ramsolutions.sw.checks;

import static nl.ramsolutions.sw.checks.CheckClassAssert.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Tests for {@link MagikTypedCheckList}. */
class MagikTypedCheckListTest {

  @Test
  void testAllChecksHaveValidAdministration() throws IOException {
    for (final Class<? extends MagikCheck> checkClass : MagikTypedCheckList.getChecks()) {
      assertThat(checkClass)
          .hasProperRuleAnnotation()
          .hasMetadata()
          .metadataRuleSpecificationMatchesClassName()
          .metadataSqKeyMatchesClassName();
    }
  }
}
