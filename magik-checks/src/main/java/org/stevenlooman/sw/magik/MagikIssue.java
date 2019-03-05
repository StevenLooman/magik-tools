package org.stevenlooman.sw.magik;

import javax.annotation.CheckForNull;

public class MagikIssue {
  private final Integer line;
  private final Integer column;
  private final String message;
  private final Double cost;
  private final MagikCheck check;

  private MagikIssue(Integer line, Integer column, String message, Double cost, MagikCheck check) {
    this.line = line;
    this.column = column;
    this.message = message;
    this.cost = cost;
    this.check = check;
  }

  public static MagikIssue fileIssue(String message, MagikCheck check) {
    return new MagikIssue(null, null, message, null, check);
  }

  public static MagikIssue lineColumnIssue(int line, int column, String message, MagikCheck check) {
    return new MagikIssue(line, column, message, null, check);
  }

  @CheckForNull
  public Integer line() {
    return line;
  }

  @CheckForNull
  public Integer column() { return column; }

  public String message() {
    return message;
  }

  @CheckForNull
  public Double cost() {
    return cost;
  }

  public MagikCheck check() {
    return check;
  }
}
