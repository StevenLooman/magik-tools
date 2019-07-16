package org.stevenlooman.sw.magik.lint.output;

import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.lint.CheckInfo;
import org.stevenlooman.sw.magik.lint.CheckInfraction;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class MessageFormatReporter extends Reporter {

  private PrintStream outStream;
  private String format;
  private Long columnOffset;

  public static final String DEFAULT_FORMAT = "${path}:${line}:${column}: ${msg} (${symbol})";

  /**
   * Constructor.
   * @param outStream Output stream to write to.
   * @param format Format to use.
   * @param columnOffset Column offset for reported columns.
   */
  public MessageFormatReporter(PrintStream outStream,
                               String format,
                               @Nullable Long columnOffset) {
    this.outStream = outStream;
    this.format = format;
    if (columnOffset != null) {
      this.columnOffset = columnOffset;
    } else {
      this.columnOffset = 0l;
    }
  }

  private Map<String, String> createMap(Path path, CheckInfo checkInfo, MagikIssue issue) throws
      FileNotFoundException {
    Map<String, String> map = new HashMap<>();
    map.put("path", path.toString());
    map.put("abspath", path.toAbsolutePath().toString());
    map.put("msg", issue.message());
    map.put("msg_id", checkInfo.getSqKey());
    map.put("symbol", checkInfo.getSqKey());
    map.put("category", checkInfo.getSeverity());

    Integer line = issue.line();
    if (line != null) {
      map.put("line", line.toString());
    }
    Integer column = issue.column();
    if (column != null) {
      column += columnOffset.intValue();
      map.put("column", column.toString());
    }

    return map;
  }

  @Override
  public void reportIssue(CheckInfraction checkInfraction) throws
      FileNotFoundException {
    Path path = checkInfraction.getPath();
    CheckInfo checkInfo = checkInfraction.getCheckInfo();
    MagikIssue magikIssue = checkInfraction.getMagikIssue();
    Map<String, String> map = createMap(path, checkInfo, magikIssue);

    String line = format.toString();
    for (Map.Entry<String, String> entry: map.entrySet()) {
      String key = "${" + entry.getKey() + "}";
      if (line.contains(key)) {
        String matchKey = Pattern.quote(key);
        String value = entry.getValue();
        String matchValue = Matcher.quoteReplacement(value);
        line = line.replaceAll(matchKey, matchValue);
      }
    }
    outStream.println(line);
  }

}
