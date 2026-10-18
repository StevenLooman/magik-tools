package nl.ramsolutions.sw.translation.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import nl.ramsolutions.sw.translation.TranslationFile;
import org.junit.jupiter.api.Test;

/** Test FileMetrics. */
@SuppressWarnings("checkstyle:MagicNumber")
class FileMetricsTest {

  @Test
  void testLines() {
    final String code =
        """
        #% text_encoding = utf8
        external_name\tpipe\tunset\ten_gb\tPipe\tWater pipe
        # comment
        external_name\tpipe\tunset\tnl_nl\tPipe\tWaterleiding

        # trailing comment
        """;
    final FileMetrics metrics = this.metrics(code);
    assertThat(metrics.commentLines()).isEqualTo(Set.of(3, 6));
    assertThat(metrics.linesOfEntries()).isEqualTo(Set.of(2, 4));
  }

  private FileMetrics metrics(final String code) {
    final TranslationFile file = new TranslationFile(TranslationFile.DEFAULT_URI, code);
    return new FileMetrics(file, true);
  }
}
