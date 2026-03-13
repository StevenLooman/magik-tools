package nl.ramsolutions.sw.magik;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Objects;

/** CodeAction. */
public class CodeAction {

  /** Code action kinds, matching LSP CodeActionKind values. */
  public static final String KIND_QUICKFIX = "quickfix";

  public static final String KIND_REFACTOR_EXTRACT = "refactor.extract";

  /** A command to be executed after the code action edits are applied. */
  public record Command(String commandId, String title, List<Object> arguments) {}

  private final String title;
  private final List<TextEdit> edits;
  private final @Nullable String kind;
  private final @Nullable Command command;

  /**
   * Constructor.
   *
   * @param title Title of action.
   * @param edit Edit for action.
   */
  public CodeAction(final String title, final TextEdit edit) {
    this(title, List.of(edit), null, null);
  }

  /**
   * Constructor.
   *
   * @param title Title of action.
   * @param edits Edits for action.
   */
  public CodeAction(final String title, final List<TextEdit> edits) {
    this(title, edits, null, null);
  }

  /**
   * Constructor.
   *
   * @param title Title of action.
   * @param edits Edits for action.
   * @param kind Code action kind (e.g., "quickfix", "refactor.extract").
   * @param command Optional command to execute after applying edits.
   */
  public CodeAction(
      final String title,
      final List<TextEdit> edits,
      final @Nullable String kind,
      final @Nullable Command command) {
    this.title = title;
    this.edits = edits;
    this.kind = kind;
    this.command = command;
  }

  public String getTitle() {
    return this.title;
  }

  public List<TextEdit> getEdits() {
    return this.edits;
  }

  /**
   * Get the code action kind.
   *
   * @return Code action kind, or null if not set (defaults to quickfix).
   */
  @CheckForNull
  public String getKind() {
    return this.kind;
  }

  /**
   * Get the command to execute after applying edits.
   *
   * @return Command, or null if not set.
   */
  @CheckForNull
  public Command getCommand() {
    return this.command;
  }

  @Override
  public String toString() {
    return "%s@%s(%s, %s)"
        .formatted(
            this.getClass().getName(),
            Integer.toHexString(this.hashCode()),
            this.getTitle(),
            this.getEdits());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.title, this.edits, this.kind, this.command);
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
    return Objects.equals(this.title, other.title)
        && Objects.equals(this.edits, other.edits)
        && Objects.equals(this.kind, other.kind)
        && Objects.equals(this.command, other.command);
  }
}
