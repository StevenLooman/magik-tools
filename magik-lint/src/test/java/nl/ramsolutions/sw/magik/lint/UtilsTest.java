package nl.ramsolutions.sw.magik.lint;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import org.junit.jupiter.api.Test;

/** Tests for {@link Utils}. */
class UtilsTest {

  @Test
  void testBuildOpenedFileNormalizesUri() {
    final Path path = Path.of("./src/test/resources/test_product/test_module/source/a.magik");
    final OpenedFile openedFile =
        Utils.buildOpenedFile(path, MagikToolsProperties.DEFAULT_PROPERTIES);

    assertThat(openedFile.getUri()).isEqualTo(path.toAbsolutePath().normalize().toUri());
  }
}
