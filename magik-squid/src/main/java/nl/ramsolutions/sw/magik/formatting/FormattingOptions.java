package nl.ramsolutions.sw.magik.formatting;

/** Formatting options for {@link FormattingStrategy}. */
public class FormattingOptions {

  private final int tabSize;
  private final boolean insertSpaces;
  private final boolean insertFinalNewline;
  private final boolean trimTrailingWhitespace;
  private final boolean trimFinalNewlines;

  public FormattingOptions(
      final int tabSize,
      final boolean insertSpaces,
      final boolean insertFinalNewline,
      final boolean trimTrailingWhitespace,
      final boolean trimFinalNewlines) {
    this.tabSize = tabSize;
    this.insertSpaces = insertSpaces;
    this.trimTrailingWhitespace = trimTrailingWhitespace;
    this.insertFinalNewline = insertFinalNewline;
    this.trimFinalNewlines = trimFinalNewlines;
  }

  public int getTabSize() {
    return this.tabSize;
  }

  public boolean isInsertSpaces() {
    return this.insertSpaces;
  }

  public boolean isInsertFinalNewline() {
    return this.insertFinalNewline;
  }

  public boolean isTrimTrailingWhitespace() {
    return this.trimTrailingWhitespace;
  }

  public boolean isTrimFinalNewlines() {
    return this.trimFinalNewlines;
  }
}
