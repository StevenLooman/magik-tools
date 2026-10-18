package nl.ramsolutions.sw.message.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import nl.ramsolutions.sw.message.MessageFile;
import org.junit.jupiter.api.Test;

/** Test FileMetrics. */
@SuppressWarnings("checkstyle:MagicNumber")
class FileMetricsTest {

  @Test
  void testLines() {
    final String code =
        """
        #% text_encoding = utf8
        # comment
        +model
        # comment
        :id_1 Text 1
        :id_2
        Text 2

        # Text 2, line 4
        """;
    final FileMetrics metrics = this.metrics(code);
    assertThat(metrics.commentLines()).isEqualTo(Set.of(4));
    assertThat(metrics.linesOfEntries()).isEqualTo(Set.of(3, 5, 6, 7, 9));
  }

  @Test
  void testCommentLinesOnlyComments() {
    final String code =
        """
        # comment 1
        # comment 2
        """;
    final FileMetrics metrics = this.metrics(code);
    assertThat(metrics.commentLines()).isEmpty();
    assertThat(metrics.linesOfEntries()).isEmpty();
  }

  private FileMetrics metrics(final String code) {
    final MessageFile file = new MessageFile(MessageFile.DEFAULT_URI, code);
    return new FileMetrics(file, true);
  }
}
