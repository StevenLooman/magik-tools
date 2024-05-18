package nl.ramsolutions.sw.magik.formatting;

import nl.ramsolutions.sw.MagikToolsProperties;

/** Settings for magik formatting. */
public class MagikFormattingSettings {

  public final String KEY_MAGIK_FORMATTING_INDENT_CHAR = "magik.formatting.indentChar";
  public final String KEY_MAGIK_FORMATTING_INDENT_WIDTH = "magik.formatting.indentWidth";
  public final String KEY_MAGIK_FORMATTING_INSERT_FINAL_NEWLINE =
      "magik.formatting.insertFinalNewline";
  public final String KEY_MAGIK_FORMATTING_TRIM_TRAILING_WHITESPACE =
      "magik.formatting.trimTrailingWhitespace";
  public final String KEY_MAGIK_FORMATTING_TRIM_FINAL_NEWLINES =
      "magik.formatting.trimFinalNewlines";

  private final MagikToolsProperties properties;

  /** Constructor. */
  public MagikFormattingSettings(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  /**
   * Get the indent character. Defaults to tab.
   *
   * @return
   */
  public char getIndentChar() {
    return this.properties.getPropertyString(KEY_MAGIK_FORMATTING_INDENT_CHAR, "tab").equals("tab")
        ? '\t'
        : ' ';
  }

  /**
   * Get the indent width.
   *
   * @return
   */
  public int getIndentWidth() {
    return this.properties.getPropertyInteger(KEY_MAGIK_FORMATTING_INDENT_WIDTH, 8);
  }

  public boolean insertFinalNewline() {
    return this.properties.getPropertyBoolean(KEY_MAGIK_FORMATTING_INSERT_FINAL_NEWLINE, true);
  }

  public boolean trimTrailingWhitespace() {
    return this.properties.getPropertyBoolean(KEY_MAGIK_FORMATTING_TRIM_TRAILING_WHITESPACE, true);
  }

  public boolean trimFinalNewlines() {
    return this.properties.getPropertyBoolean(KEY_MAGIK_FORMATTING_TRIM_FINAL_NEWLINES, true);
  }

  public String getIndent() {
    final char indentChar = this.getIndentChar();
    if (indentChar == '\t') {
      return String.valueOf(indentChar);
    }

    final int indentWidth = this.getIndentWidth();
    return String.valueOf(indentChar).repeat(indentWidth);
  }
}
