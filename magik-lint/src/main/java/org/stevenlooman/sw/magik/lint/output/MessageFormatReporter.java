package org.stevenlooman.sw.magik.lint.output;

import com.google.common.collect.Maps;
import org.apache.commons.text.StringSubstitutor;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.lint.CheckInfo;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;

public class MessageFormatReporter extends Reporter {

  private String format;

//  public final static String DEFAULT_FORMAT = "${path}:${line}:${column}: ${msg_id}: ${msg} (${symbol})";
  public final static String DEFAULT_FORMAT = "${path}:${line}:${column}: ${msg} (${symbol})";

  public MessageFormatReporter() {
    this(DEFAULT_FORMAT);
  }

  public MessageFormatReporter(String format) {
    this.format = format;
  }

  private Map<String, String> createMap(Path path, CheckInfo checkInfo, MagikIssue issue) throws FileNotFoundException {
    Map<String, String> map = Maps.newHashMap();
    map.put("path", path.toString());
    map.put("abspath", path.toAbsolutePath().toString());
    map.put("msg", issue.message());
    map.put("msg_id", checkInfo.getName());
    map.put("symbol", checkInfo.getName());
    map.put("category", checkInfo.getSeverity());

    Integer line = issue.line();
    if (line != null) {
      map.put("line", line.toString());
    }
    Integer column = issue.column();
    if (column != null) {
      map.put("column", column.toString());

    }

    return map;
  }

  @Override
  public void reportIssue(Path path, CheckInfo checkInfo, MagikIssue issue) throws FileNotFoundException {
    Map<String, String> map = createMap(path, checkInfo, issue);
    StringSubstitutor sub = new StringSubstitutor(map);
    String resolvedString = sub.replace(format);
    System.out.println(resolvedString);
  }

}
