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
  public void applyCodeAction(final CodeAction codeAction) {
    final Comparator<TextEdit> byEndPosition =
        Comparator.comparing(textEdit -> textEdit.getRange().getEndPosition());
    codeAction.getEdits().stream()
        .sorted(byEndPosition.reversed()) // Apply edits from back to front.
        .forEach(this::applyTextEdit);
  }

  private void applyTextEdit(final TextEdit textEdit) {
    final String[] lines = this.source.split("\r\n|\n|\r");
    final Range range = textEdit.getRange();
    final Position startPosition = range.getStartPosition();
    final Position endPosition = range.getEndPosition();
    final int startPositionIndex = this.getIndexOfPosition(lines, startPosition);
    final int endPositionIndex = this.getIndexOfPosition(lines, endPosition);
    this.source =
        this.source.substring(0, startPositionIndex)
            + textEdit.getNewText()
            + this.source.substring(endPositionIndex);
  }

  private int getIndexOfPosition(final String[] lines, final Position position) {
    final int line = position.getLine();
    final int column = position.getColumn();
    int index = 0;
    for (int i = 0; i < line - 1; i++) {
      index += lines[i].length();
    }
    index += column;
    return index;
  }
}
