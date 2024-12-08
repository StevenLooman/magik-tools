package nl.ramsolutions.sw.magik;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;

/** A text edit. */
public class TextEdit {

  private final Range range;
  private final String newText;
  private final @Nullable String reason;

  public TextEdit(final Range range, final String newText) {
    this.range = range;
    this.newText = newText;
    this.reason = null;
  }

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
