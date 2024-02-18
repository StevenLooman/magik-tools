package nl.ramsolutions.sw.magik;

import java.util.List;
import java.util.Objects;

/** CodeAction. */
public class CodeAction {

  private final String title;
  private final List<TextEdit> edits;

  /**
   * Constructor.
   *
   * @param title Title of action.
   * @param edit Edit for action.
   */
  public CodeAction(final String title, final TextEdit edit) {
    this(title, List.of(edit));
  }

  /**
   * Constructor.
   *
   * @param title Title of action.
   * @param edits Edits for action.
   */
  public CodeAction(final String title, final List<TextEdit> edits) {
    this.title = title;
    this.edits = edits;
  }

  public String getTitle() {
    return this.title;
  }

  public List<TextEdit> getEdits() {
    return this.edits;
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s, %s)",
        this.getClass().getName(),
        Integer.toHexString(this.hashCode()),
        this.getTitle(),
        this.getEdits());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.title, this.edits);
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

    final CodeAction other = (CodeAction) obj;
    return Objects.equals(this.title, other.title) && Objects.equals(this.edits, other.edits);
  }
}
