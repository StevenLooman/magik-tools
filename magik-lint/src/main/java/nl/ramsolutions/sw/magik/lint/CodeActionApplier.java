package nl.ramsolutions.sw.magik.lint;

import java.util.Comparator;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;

/** {@link CodeAction} applier to source. */
public class CodeActionApplier {

  private String source;

  /**
   * Constructor.
   *
   * @param source Source.
   */
  public CodeActionApplier(final String source) {
    this.source = source;
  }

  /**
   * Get the source.
   *
   * @return Source.
   */
  public String getSource() {
    return this.source;
  }

  /**
   * Apply a {@link CodeAction} to the current source.
   *
   * @param codeAction CodeAction to apply.
   */
  public void apply(final CodeAction codeAction) {
    final Comparator<TextEdit> byEndPosition =
        Comparator.comparing(textEdit -> textEdit.getRange().getEndPosition());
    codeAction.getEdits().stream()
        .sorted(byEndPosition.reversed()) // Apply edits from back to front.
        .forEach(this::apply);
  }

  /**
   * Apply a {@link TextEdit} to the current source.
   *
   * @param textEdit TextEdit to apply.
   */
  public void apply(final TextEdit textEdit) {
    final Range range = textEdit.getRange();
    final Position startPosition = range.getStartPosition();
    final Position endPosition = range.getEndPosition();
    final int startPositionIndex = this.getIndexOfPosition(source, startPosition);
    final int endPositionIndex = this.getIndexOfPosition(source, endPosition);
    this.source =
        this.source.substring(0, startPositionIndex)
            + textEdit.getNewText()
            + this.source.substring(endPositionIndex);
  }

  private int getIndexOfPosition(final String source, final Position position) {
    final int line = position.getLine();
    final int column = position.getColumn();

    int currentLine = 1;
    int index = 0;
    // Find first line index.
    while (index < source.length()) {
      if (currentLine == line) {
        break;
      }

      // Find next newline character.
      final int nextIndex = source.indexOf("\n", index == 0 ? 0 : index + 1);
      if (nextIndex == -1) {
        // Apparently at EOF, lets not crash and just take the end of the string.
        return source.length();
      }

      index = nextIndex;
      currentLine += 1;
    }

    if (currentLine == 1) {
      return column;
    }

    return Integer.min(index + 1 + column, source.length());
  }
}
