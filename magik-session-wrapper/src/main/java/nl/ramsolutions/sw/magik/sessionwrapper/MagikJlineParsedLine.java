package nl.ramsolutions.sw.magik.sessionwrapper;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import org.jline.reader.ParsedLine;

/** ParsedLine implementation for Magik. */
class MagikJlineParsedLine implements ParsedLine {

  private final String line;
  private final int cursor;
  private final AstNode topNode;

  /**
   * Constructor.
   *
   * @param line Line is the text that was entered.
   * @param cursor Cursor is the cursor position in the line, starting at 0.
   * @param topNode AstNode is the root node of the AST.
   */
  MagikJlineParsedLine(final String line, final int cursor, final AstNode topNode) {
    this.line = line;
    this.cursor = cursor;
    this.topNode = topNode;
  }

  @Override
  public String word() {
    // Try getting the word via the AST. This requires that there is no syntax error.
    final Position cursorPosition = this.getPositionFromCursor();
    if (cursorPosition.getColumn() == 0) {
      return "";
    }

    final int previousLine = cursorPosition.getLine();
    final int previousColumn = cursorPosition.getColumn() - 1;
    final Position previousPosition = new Position(previousLine, previousColumn);
    final AstNode node = AstQuery.nodeAt(this.topNode, previousPosition);
    if (node == null) {
      return "";
    }

    final Token token = node.getToken();
    Objects.requireNonNull(token);

    return token.getOriginalValue();
  }

  @Override
  public int wordCursor() {
    final Position cursorPosition = this.getPositionFromCursor();
    final AstNode node = AstQuery.nodeAt(topNode, cursorPosition);
    if (node == null) {
      return 0;
    }

    final Token token = node.getToken();
    Objects.requireNonNull(token);

    final Position tokenStartPosition = Position.fromTokenStart(token);
    return cursorPosition.getColumn() - tokenStartPosition.getColumn();
  }

  @Override
  public int wordIndex() {
    // TODO?
    return 0;
  }

  @Override
  public List<String> words() {
    // TODO?
    return List.of(this.word());
  }

  @Override
  public String line() {
    return this.line;
  }

  @Override
  public int cursor() {
    return this.cursor;
  }

  private Position getPositionFromCursor() {
    int positionLine = 1;
    int positionColumn = 0;
    final int maxIndex = Math.min(this.cursor, this.line.length());
    for (int i = 0; i < maxIndex; i++) {
      if (this.line.charAt(i) == '\n') {
        positionLine++;
        positionColumn = 0;
      } else {
        positionColumn++;
      }
    }

    return new Position(positionLine, positionColumn);
  }
}
