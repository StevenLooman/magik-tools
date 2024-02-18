package nl.ramsolutions.sw.magik.checks;

import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Range;

/** Magik issue, resulting from a MagikCheck. */
public class MagikIssue {

  private final Location location;
  private final String message;
  private final MagikCheck check;

  /**
   * Constructor.
   *
   * @param location Location of issue.
   * @param message Message for issue.
   * @param check Check giving the issue.
   */
  public MagikIssue(final Location location, final String message, final MagikCheck check) {
    this.location = location;
    this.message = message;
    this.check = check;
  }

  /**
   * Get the {@link Location} of the issue.
   *
   * @return {@link Location}.
   */
  public Location location() {
    return this.location;
  }

  /**
   * Get the {@link Range} of the issue.
   *
   * @return Return {@link Range} if non null, else defaults to {@link DEFAULT_RANGE}.
   */
  public Range range() {
    final Range range = this.location.getRange();
    return range != null ? range : Range.DEFAULT_RANGE;
  }

  /**
   * Get the start line of the issue, 1-based.
   *
   * @return Start line of the issue, defaults to 1 if no range.
   */
  public int startLine() {
    final Range range = this.range();
    return range.getStartPosition().getLine();
  }

  /**
   * Get the start column of the issue.
   *
   * @return Start column of the issue, defaults to 0 if no range.
   */
  public int startColumn() {
    final Range range = this.range();
    return range.getStartPosition().getColumn();
  }

  /**
   * Get the end line of the issue, 1-based.
   *
   * @return End line of the issue, defaults to 1 if no range.
   */
  public int endLine() {
    final Range range = this.range();
    return range.getEndPosition().getLine();
  }

  /**
   * Get the end column of the issue.
   *
   * @return End column of the issue, defaults to 0 if no range.
   */
  public int endColumn() {
    final Range range = this.range();
    return range.getEndPosition().getColumn();
  }

  /**
   * Get the message for the issue.
   *
   * @return Message for the issue.
   */
  public String message() {
    return this.message;
  }

  /**
   * Get the {@link MagikCheck} giving the issue.
   *
   * @return {@link MagikCheck} giving the issue.
   */
  public MagikCheck check() {
    return this.check;
  }
}
