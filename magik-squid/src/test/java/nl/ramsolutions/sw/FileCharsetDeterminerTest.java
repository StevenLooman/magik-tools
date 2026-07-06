package nl.ramsolutions.sw;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Test {@link FileCharsetDeterminer}. */
class FileCharsetDeterminerTest {

  /**
   * Get path to file. Handle vscode/mvn inconsistencies.
   *
   * @param relativePath Relative path to file.
   * @return Full path to file.
   */
  protected Path getPath(final Path relativePath) {
    final Path path = Path.of(".").toAbsolutePath().getParent();
    if (path.endsWith("magik-checks")) {
      return Path.of("..").resolve(relativePath);
    }
    return Path.of(".").resolve(relativePath);
  }

  @Test
  void testParseIso88591() {
    final Path path =
        this.getPath(Path.of("src/test/resources/tests/parser/determine_encoding_1.magik"));
    final Charset result = FileCharsetDeterminer.determineCharset(path);

    assertThat(result).isEqualTo(StandardCharsets.ISO_8859_1);
  }

  @Test
  void testParseUtf8() {
    final Path path =
        this.getPath(Path.of("src/test/resources/tests/parser/determine_encoding_2.magik"));
    final Charset result = FileCharsetDeterminer.determineCharset(path);

    assertThat(result).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  void testMissingFile() {
    final Path path = Path.of("non-existent.magik");
    final Charset result = FileCharsetDeterminer.determineCharset(path);

    assertThat(result).isEqualTo(StandardCharsets.ISO_8859_1);
  }

  @Test
  void testParseNoSpaces() {
    final Charset result = FileCharsetDeterminer.determineCharset("#%text_encoding=utf8\n");

    assertThat(result).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  void testParseMixedSpacing() {
    final Charset result = FileCharsetDeterminer.determineCharset("#% text_encoding=iso8859_1\n");

    assertThat(result).isEqualTo(StandardCharsets.ISO_8859_1);
  }

  @Test
  void testHasEncodingDeclarationVariants() {
    assertThat(FileCharsetDeterminer.hasEncodingDeclaration("#% text_encoding = utf8")).isTrue();
    assertThat(FileCharsetDeterminer.hasEncodingDeclaration("#%text_encoding=utf8")).isTrue();
    assertThat(FileCharsetDeterminer.hasEncodingDeclaration("_package user")).isFalse();
    assertThat(FileCharsetDeterminer.hasEncodingDeclaration(null)).isFalse();
  }
}
