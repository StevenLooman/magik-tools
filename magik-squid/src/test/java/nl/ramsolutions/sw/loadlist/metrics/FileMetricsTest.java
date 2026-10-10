package nl.ramsolutions.sw.loadlist.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import org.junit.jupiter.api.Test;

/** Test FileMetrics. */
@SuppressWarnings("checkstyle:MagicNumber")
class FileMetricsTest {

  @Test
  void testCommentLinesTrailingComments() {
    final String code =
        """
        # header comment
        file_a
        # comment
        file_b
        # trailing comment 1
        # trailing comment 2
        """;
    final FileMetrics metrics = this.metrics(code);
    assertThat(metrics.commentLines()).isEqualTo(Set.of(3, 5, 6));
    assertThat(metrics.linesOfEntries()).isEqualTo(Set.of(2, 4));
  }

  @Test
  void testCommentLinesOnlyComments() {
    // A file with only comments has only a header comment.
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
    final LoadListFile file = new LoadListFile(LoadListFile.DEFAULT_URI, code);
    return new FileMetrics(file, true);
  }
}
