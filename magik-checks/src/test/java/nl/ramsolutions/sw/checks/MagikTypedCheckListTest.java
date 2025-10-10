package nl.ramsolutions.sw.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** Tests for {@link MagikTypedCheckList}. */
class MagikTypedCheckListTest {

  @Test
  void testAllChecksHaveJsonFile() throws IOException {
    for (Class<? extends MagikCheck> checkClass : MagikTypedCheckList.getChecks()) {
      final CheckHolder holder = new CheckHolder(checkClass, Collections.emptySet(), true);
      final CheckMetadata metadata = holder.getMetadata();
      assertThat(metadata).isNotNull();

      final String ruleKeySpecification = metadata.getRuleSpecification();
      final String simpleName = checkClass.getSimpleName().replaceAll("TypedCheck$", "");
      assertThat(ruleKeySpecification).isEqualTo(simpleName);

      final String sqKey = metadata.getSqKey();
      final String simpleNameKebabCase = MagikTypedCheckListTest.toKebabCase(sqKey);
      assertThat(sqKey).isEqualTo(simpleNameKebabCase);
    }
  }

  private static String toKebabCase(final String string) {
    final String stringKebab =
        string.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    if (stringKebab.startsWith("-")) {
      return stringKebab.substring(1);
    }
    return stringKebab;
  }
}
