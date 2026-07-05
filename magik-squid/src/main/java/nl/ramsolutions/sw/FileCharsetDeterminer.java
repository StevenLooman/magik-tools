package nl.ramsolutions.sw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File charset determiner.
 *
 * <p>Reads the top line: #% text_encoding = &lt;encoding&gt;
 */
public final class FileCharsetDeterminer {

  private static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

  /**
   * Pattern for the source-encoding declaration, tolerant of the spacing Smallworld accepts: {@code
   * #% text_encoding = utf8} as well as {@code #%text_encoding=utf8}.
   */
  private static final Pattern ENCODING_PATTERN =
      Pattern.compile("#%\\s*text_encoding\\s*=\\s*(\\S+)");

  private FileCharsetDeterminer() {}

  /**
   * Try to determine the charset used in this file. Magik files usually contain a line specifying
   * the encoding: #% text_encoding = iso8859_1
   *
   * @param path Path to file to check
   * @return Charset for file or <code>defaultCharset</code> if undetermined
   */
  public static Charset determineCharset(final Path path) {
    try (BufferedReader bufferedReader =
        Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
      final String line = bufferedReader.readLine();
      return FileCharsetDeterminer.readCharsetFromLine(line);
    } catch (final IllegalArgumentException | IOException exception) {
      // do nothing
    }

    return DEFAULT_CHARSET;
  }

  /**
   * Try to determine the charset used in this file. Magik files usually contain a line specifying
   * the encoding: #% text_encoding = iso8859_1
   *
   * @param source Source to read text_encoding from.
   * @return Charset for file or <code>defaultCharset</code> if undetermined
   */
  public static Charset determineCharset(final String source) {
    final StringReader stringReader = new StringReader(source);
    try (BufferedReader bufferedReader = new BufferedReader(stringReader)) {
      final String line = bufferedReader.readLine();
      return FileCharsetDeterminer.readCharsetFromLine(line);
    } catch (final IllegalArgumentException | IOException exception) {
      // do nothing
    }

    return DEFAULT_CHARSET;
  }

  /**
   * Check whether a line is a Magik source-encoding declaration ({@code #% text_encoding = ...}),
   * tolerating the optional whitespace Smallworld accepts around the tokens.
   *
   * @param line Line to check (may be {@code null}).
   * @return {@code true} if the line declares a text encoding.
   */
  public static boolean hasEncodingDeclaration(final String line) {
    return line != null && FileCharsetDeterminer.ENCODING_PATTERN.matcher(line).lookingAt();
  }

  private static Charset readCharsetFromLine(final String line) {
    if (line != null) {
      final Matcher matcher = FileCharsetDeterminer.ENCODING_PATTERN.matcher(line);
      if (matcher.lookingAt()) {
        return Charset.forName(matcher.group(1));
      }
    }
    return DEFAULT_CHARSET;
  }
}
