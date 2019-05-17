package org.stevenlooman.sw.magik.lint;

import org.stevenlooman.sw.magik.MagikIssue;

import java.nio.file.Path;

public class CheckInfraction {

  private final Path path;
  private final CheckInfo checkInfo;
  private final MagikIssue magikIssue;

  /**
   * Constructor.
   * @param path Path to file.
   * @param checkInfo Check info of check.
   * @param magikIssue Issue of file/check.
   */
  public CheckInfraction(Path path, CheckInfo checkInfo, MagikIssue magikIssue) {
    this.path = path;
    this.checkInfo = checkInfo;
    this.magikIssue = magikIssue;
  }

  /**
   * Get Path to infraction.
   * @return Path to infraction.
   */
  public Path getPath() {
    return path;
  }

  /**
   * Get CheckInfo to infraction.
   * @return CheckInfo to infraction.
   */
  public CheckInfo getCheckInfo() {
    return checkInfo;
  }

  /**
   * Get MagikIssue to infraction.
   * @return MagikIssue to infraction.
   */
  public MagikIssue getMagikIssue() {
    return magikIssue;
  }

}
