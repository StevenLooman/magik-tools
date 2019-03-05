package org.stevenlooman.sw.magik;

public class MagikCommentAnalyser {

  private MagikCommentAnalyser() {
    // Cannot be instantiated.
  }

  /**
   * Check if line is a blank line.
   * @param line Line to check.
   * @return True if line is blank, else false.
   */
  public static boolean isBlank(String line) {
    for (int i = 0; i < line.length(); i++) {
      if (Character.isLetterOrDigit(line.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get contents of comment.
   * @param comment Comment to get contents from.
   * @return Text of comment.
   */
  public static String getContents(String comment) {
    // Comments always starts with "#"
    return comment.substring(comment.indexOf('#'));
  }

}
