package nl.ramsolutions.sw.checks.magik;

import static nl.ramsolutions.sw.checks.magik.MagikCheckAssert.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.checks.MagikCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests {@link PragmaDoesNotIncludeTopicInTopicsCheck}. */
public class PragmaDoesNotIncludeTopicInTopicsCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "magik-checks/src/test/resources/test_product_2/modules/test_module_2/source/test_exemplar.magik",
        "magik-checks/src/test/resources/test_product/modules/test_module/source/test_exemplar.magik"
      })
  void testValid(final String path) throws IOException {
    final MagikCheck check = new PragmaDoesNotIncludeTopicInTopicsCheck();
    final Path filePath = Path.of(path);
    assertThat(check).reportsNoIssues(filePath);
  }

  @Test
  void testInvalid() throws IOException {
    final PragmaDoesNotIncludeTopicInTopicsCheck check =
        new PragmaDoesNotIncludeTopicInTopicsCheck();
    check.topics = "test_topic";
    final Path path =
        Path.of(
            "magik-checks/src/test/resources/test_product/modules/test_module/source/test_exemplar.magik");
    assertThat(check).reportsIssueCount(path, 1);
  }
}
