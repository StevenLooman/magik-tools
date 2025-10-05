package nl.ramsolutions.sw.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** Tests for {@link ProductDefCheckList}. */
class ProductDefCheckListTest {

  @Test
  void testAllChecksHaveJsonFile() throws IOException {
    for (Class<? extends ProductDefCheck> checkClass : ProductDefCheckList.getChecks()) {
      final CheckHolder holder = new CheckHolder(checkClass, Collections.emptySet(), true);
      final CheckMetadata metadata = holder.getMetadata();
      assertThat(metadata).isNotNull();
    }
  }
}
