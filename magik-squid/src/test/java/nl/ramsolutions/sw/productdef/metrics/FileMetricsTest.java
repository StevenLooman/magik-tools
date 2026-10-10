package nl.ramsolutions.sw.productdef.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.junit.jupiter.api.Test;

/** Test FileMetrics. */
@SuppressWarnings("checkstyle:MagicNumber")
class FileMetricsTest {

  @Test
  void testCommentLinesTrailingComments() {
    final String code =
        """
        # header comment
        prod_a layered_product
        # comment
        description
            test product
        end
        # trailing comment 1
        # trailing comment 2
        """;
    final FileMetrics metrics = this.metrics(code);
    assertThat(metrics.commentLines()).isEqualTo(Set.of(3, 7, 8));
    assertThat(metrics.linesOfDefinition()).isEqualTo(Set.of(2, 4, 5, 6));
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
    assertThat(metrics.linesOfDefinition()).isEmpty();
  }

  private FileMetrics metrics(final String code) {
    final ProductDefFile file =
        new ProductDefFile(ProductDefFile.DEFAULT_URI, code, new DefinitionKeeper(), null);
    return new FileMetrics(file, true);
  }
}
