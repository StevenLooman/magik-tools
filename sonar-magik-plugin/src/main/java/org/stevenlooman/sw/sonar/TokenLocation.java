package org.stevenlooman.sw.sonar;

import com.sonar.sslr.api.Token;

public class TokenLocation {

  private Token token;

  /**
   * Constructor.
   * @param token Token to wrap.
   */
  public TokenLocation(Token token) {
    this.token = token;
  }

  /**
   * Get line.
   * @return Line.
   */
  public int line() {
    return token.getLine();
  }

  /**
   * Get column.
   * @return Column.
   */
  public int column() {
    return token.getColumn();
  }

  /**
   * Get end line.
   * @return End line.
   */
  public int endLine() {
    String[] lines = token.getValue().split("\\r\\n|\\n|\\r");
    return token.getLine() + lines.length - 1;
  }

  /**
   * Get end column.
   * @return End column.
   */
  public int endColumn() {
    int endLineOffset = token.getColumn() + token.getValue().length();
    if (endLine() != token.getLine()) {
      String[] lines = token.getValue().split("\\r\\n|\\n|\\r");
      endLineOffset = lines[lines.length - 1].length();
    }

    return endLineOffset;
  }

}
