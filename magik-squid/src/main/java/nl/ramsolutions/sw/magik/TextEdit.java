package nl.ramsolutions.sw.magik;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;

/**
 * A text edit.
 *
 * <p>To quote the <a
 * href="https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textEditArray">LSP
 * specification 3.17</a>: All text edits ranges refer to positions in the document they are
 * computed on. They therefore move a document from state S1 to S2 without describing any
 * intermediate state.
 */
public class TextEdit {

  private final Range range;
  private final String newText;
  private final @Nullable String reason;

  /**
   * Create a new text edit.
   *
   * @param range Range of the text edit.
   * @param newText New text to insert.
   */
  public TextEdit(final Range range, final String newText) {
    this.range = range;
    this.newText = newText;
    this.reason = null;
  }

  /**
   * Create a new text edit.
   *
   * @param range Range of the text edit.
   * @param newText New text to insert.
   * @param reason Reason for the text edit.
   */
  public TextEdit(final Range range, final String newText, final String reason) {
    this.range = range;
    this.newText = newText;
    this.reason = reason;
  }

  public Range getRange() {
    return this.range;
  }

  public String getNewText() {
    return this.newText;
  }

  @CheckForNull
  public String getReason() {
    return this.reason;
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s, '%s', %s)",
        this.getClass().getName(),
        Integer.toHexString(this.hashCode()),
        this.getRange(),
        this.getNewText(),
        this.getReason());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.range, this.newText);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final TextEdit other = (TextEdit) obj;
    return Objects.equals(this.range, other.range)
        && Objects.equals(this.newText, other.newText)
        && Objects.equals(this.reason, other.reason);
  }
}
