package nl.ramsolutions.sw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * File charset determiner.
 *
 * <p>Reads the top line: #% text_encoding = <encoding>
 */
public final class FileCharsetDeterminer {

  private static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;
  private static final String ENCODING_LINE = "#% text_encoding =";

  private FileCharsetDeterminer() {}

  /**
   * Try to determine the charset used in this file. Magik files usually contain a line specifying
   * the encoding: #% text_encoding = iso8859_1
   *
   * @param path Path to file to check
   * @return Charset for file or <code>defaultCharset</code> if undetermined
   */
  public static Charset determineCharset(final Path path) {
    final File file = path.toFile();
    try (FileReader fileReader = new FileReader(file, StandardCharsets.ISO_8859_1);
        BufferedReader bufferedReader = new BufferedReader(fileReader)) {
      final String line = bufferedReader.readLine();
      return FileCharsetDeterminer.readCharsetFromLine(line);
    } catch (IllegalArgumentException | IOException exception) {
      // do nothing
    }

    return DEFAULT_CHARSET;
  }

  /**
   * Try to determine the charset used in this file. Magik files usually contain a line specifying
   * the encoding: #% text_encoding = iso8859_1
   *
   * @param source Source to raed text_encoding from.
   * @return Charset for file or <code>defaultCharset</code> if undetermined
   */
  public static Charset determineCharset(final String source) {
    final StringReader stringReader = new StringReader(source);
    try (BufferedReader bufferedReader = new BufferedReader(stringReader)) {
      final String line = bufferedReader.readLine();
      return FileCharsetDeterminer.readCharsetFromLine(line);
    } catch (IllegalArgumentException | IOException exception) {
      // do nothing
    }

    return DEFAULT_CHARSET;
  }

  private static Charset readCharsetFromLine(final String line) {
    if (line != null && line.startsWith(ENCODING_LINE)) {
      final String encoding = line.substring(ENCODING_LINE.length()).trim();
      return Charset.forName(encoding);
    }
    return DEFAULT_CHARSET;
  }
}
