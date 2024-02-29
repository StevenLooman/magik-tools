package nl.ramsolutions.sw.magik.typedchecks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikCheckHolder;
import nl.ramsolutions.sw.magik.checks.MagikCheckMetadata;
import org.junit.jupiter.api.Test;

/** Tests for {@link CheckList}. */
class CheckListTest {

  @Test
  void testAllChecksHaveAJsonFile() throws IOException {
    for (Class<? extends MagikCheck> checkClass : CheckList.getChecks()) {
      final MagikCheckHolder holder =
          new MagikCheckHolder(checkClass, Collections.emptySet(), true);
      final MagikCheckMetadata metadata = holder.getMetadata();
      assertThat(metadata).isNotNull();
    }
  }
}
