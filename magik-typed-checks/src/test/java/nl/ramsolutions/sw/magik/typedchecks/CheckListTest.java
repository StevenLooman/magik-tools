package nl.ramsolutions.sw.magik.typedchecks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import nl.ramsolutions.sw.magik.checks.CheckHolder;
import nl.ramsolutions.sw.magik.checks.CheckMetadata;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.junit.jupiter.api.Test;

/** Tests for {@link CheckList}. */
class CheckListTest {

  @Test
  void testAllChecksHaveAJsonFile() throws IOException {
    for (Class<? extends MagikCheck> checkClass : CheckList.getChecks()) {
      final CheckHolder holder = new CheckHolder(checkClass, Collections.emptySet(), true);
      final CheckMetadata metadata = holder.getMetadata();
      assertThat(metadata).isNotNull();
    }
  }
}
