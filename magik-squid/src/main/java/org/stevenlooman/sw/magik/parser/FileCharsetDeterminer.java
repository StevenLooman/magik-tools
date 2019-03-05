package org.stevenlooman.sw.magik.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import javax.annotation.Nullable;

public class FileCharsetDeterminer {

  private static final String ENCODING_LINE = "#% text_encoding =";

  private FileCharsetDeterminer() {

  }

  /**
   * Try to determine the charset used in this file.
   * Magik files usually contain a line specifying the encoding: #% text_encoding = iso8859_1
   * @param path Path to file to check
   * @param defaultCharset Default charset to return
   * @return Charset for file or <code>defaultCharset</code> if undetermined
   */

  public static Charset determineCharset(Path path, @Nullable Charset defaultCharset) {
    try (FileReader fileReader = new FileReader(path.toFile());
         BufferedReader bufferedReader = new BufferedReader(fileReader)) {
      String line = bufferedReader.readLine();
      if (line != null) {
        if (line.startsWith(ENCODING_LINE)) {
          line = line.substring(ENCODING_LINE.length()).trim();
          return Charset.forName(line);
        }
      }
    }
    catch (IllegalArgumentException ex) {
      // do nothing
    } catch (IOException ex) {
      // do nothing
    }

    return defaultCharset;
  }

}
