package nl.ramsolutions.sw.magik.lint;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.junit.jupiter.api.Test;

/** Test MagikFileScanner. */
class MagikFileScannerTest {

  /**
   * VSCode runs from module directory, mvn runs from project directory.
   *
   * @return Proper {@link Path} to use.
   */
  private Path getPath(String relativePath) {
    final Path path = Path.of(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-lint")) {
      return Path.of("..").resolve(relativePath);
    }
    return Path.of(".").resolve(relativePath);
  }

  @Test
  void testScanFiles() throws IOException {
    final Path testProductPath = this.getPath("magik-lint/src/test/resources/test_product");
    final Collection<Path> actualPathsCollection = MagikFileScanner.scanMagikFiles(testProductPath);

    assertThat(actualPathsCollection)
        .containsExactlyInAnyOrder(
            this.getPath("magik-lint/src/test/resources/test_product/test_module/source/a.magik"),
            this.getPath("magik-lint/src/test/resources/test_product/test_module/source/b.magik"),
            this.getPath(
                "magik-lint/src/test/resources/test_product/test_module/source/sw/integer.magik"));
  }

  @Test
  void testgetFilesFromArgsSingleFile() throws IOException {
    final Path path =
        this.getPath("magik-lint/src/test/resources/test_product/test_module/source/a.magik");
    final String[] args = new String[] {path.toString()};
    final Collection<Path> actual = MagikFileScanner.getFilesFromArgs(args);

    assertThat(actual)
        .containsExactly(
            this.getPath("magik-lint/src/test/resources/test_product/test_module/source/a.magik"));
  }

  @Test
  void testgetFilesFromArgsDirectory() throws IOException {
    final Path path = this.getPath("magik-lint/src/test/resources/test_product/test_module/source");
    final String[] args = new String[] {path.toString()};
    final Collection<Path> actual = MagikFileScanner.getFilesFromArgs(args);
    assertThat(actual)
        .containsExactlyInAnyOrder(
            this.getPath("magik-lint/src/test/resources/test_product/test_module/source/a.magik"),
            this.getPath("magik-lint/src/test/resources/test_product/test_module/source/b.magik"),
            this.getPath(
                "magik-lint/src/test/resources/test_product/test_module/source/sw/integer.magik"));
  }
}
