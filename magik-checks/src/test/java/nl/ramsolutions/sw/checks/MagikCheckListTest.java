package nl.ramsolutions.sw.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** Tests for {@link MagikCheckList}. */
class MagikCheckListTest {

  @Test
  void testAllChecksHaveJsonFile() throws IOException {
    for (Class<? extends MagikCheck> checkClass : MagikCheckList.getChecks()) {
      final CheckHolder holder = new CheckHolder(checkClass, Collections.emptySet(), true);
      final CheckMetadata metadata = holder.getMetadata();
      assertThat(metadata).isNotNull();

      final String ruleKeySpecification = metadata.getRuleSpecification();
      final String simpleName = checkClass.getSimpleName().replaceAll("Check$", "");
      assertThat(ruleKeySpecification).isEqualTo(simpleName);

      final String sqKey = metadata.getSqKey();
      final String simpleNameKebabCase = MagikCheckListTest.toKebabCase(sqKey);
      assertThat(sqKey).isEqualTo(simpleNameKebabCase);
    }
  }

  @Test
  void testAllChecksHaveHtmlFile() throws IOException {
    for (Class<? extends MagikCheck> checkClass : MagikCheckList.getChecks()) {
      final String simpleName = checkClass.getSimpleName().replaceAll("Check$", "");

      // Get path to HTML file.
      final String htmlFileName = simpleName + ".html";
      final String htmlFilePath = "/" + MagikCheckList.PROFILE_DIR + "/" + htmlFileName;
      try (final InputStream htmlFileStream =
          MagikCheckListTest.class.getResourceAsStream(htmlFilePath)) {
        assertThat(htmlFileStream)
            .withFailMessage(
                "No HTML file found for check %s at path %s", checkClass.getName(), htmlFilePath)
            .isNotNull();
      }
    }
  }

  private static String toKebabCase(final String string) {
    final String stringKebab = string.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    if (stringKebab.startsWith("-")) {
      return stringKebab.substring(1);
    }
    return stringKebab;
  }
}
