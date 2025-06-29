package nl.ramsolutions.sw.magik.formatting;

import nl.ramsolutions.sw.MagikToolsProperties;

/** Settings for magik formatting. */
public class MagikFormattingSettings {

  public static final String KEY_MAGIK_FORMATTING_INDENT_STRATEGY =
      "magik.formatting.indentStrategy";
  public static final String KEY_MAGIK_FORMATTING_INDENT_CHAR = "magik.formatting.indentChar";
  public static final String KEY_MAGIK_FORMATTING_INDENT_WIDTH = "magik.formatting.indentWidth";
  public static final String KEY_MAGIK_FORMATTING_INSERT_FINAL_NEWLINE =
      "magik.formatting.insertFinalNewline";
  public static final String KEY_MAGIK_FORMATTING_TRIM_TRAILING_WHITESPACE =
      "magik.formatting.trimTrailingWhitespace";
  public static final String KEY_MAGIK_FORMATTING_TRIM_FINAL_NEWLINES =
      "magik.formatting.trimFinalNewlines";

  private final MagikToolsProperties properties;

  /** Constructor. */
  public MagikFormattingSettings(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  /**
   * Get the indent strategy.
   *
   * @return The indent strategy, defaults to "default".
   */
  public String getIndentStrategy() {
    return this.properties.getPropertyString(KEY_MAGIK_FORMATTING_INDENT_STRATEGY, "null");
  }

  /**
   * Get the indent character. Defaults to tab.
   *
   * @return The indent character, either a tab or a space.
   */
  public char getIndentChar() {
    return this.properties.getPropertyString(KEY_MAGIK_FORMATTING_INDENT_CHAR, "tab").equals("tab")
        ? '\t'
        : ' ';
  }

  /**
   * Get the indent width.
   *
   * @return The indent width, defaults to 8.
   */
  public int getIndentWidth() {
    return this.properties.getPropertyInteger(KEY_MAGIK_FORMATTING_INDENT_WIDTH, 8);
  }

  /**
   * Check if the formatter should insert a final newline.
   *
   * @return True if a final newline should be inserted, defaults to true.
   */
  public boolean insertFinalNewline() {
    return this.properties.getPropertyBoolean(KEY_MAGIK_FORMATTING_INSERT_FINAL_NEWLINE, true);
  }

  /**
   * Check if the formatter should trim trailing whitespace.
   *
   * @return True if trailing whitespace should be trimmed, defaults to true.
   */
  public boolean trimTrailingWhitespace() {
    return this.properties.getPropertyBoolean(KEY_MAGIK_FORMATTING_TRIM_TRAILING_WHITESPACE, true);
  }

  /**
   * Check if the formatter should trim final newlines.
   *
   * @return True if final newlines should be trimmed, defaults to true.
   */
  public boolean trimFinalNewlines() {
    return this.properties.getPropertyBoolean(KEY_MAGIK_FORMATTING_TRIM_FINAL_NEWLINES, true);
  }

  /**
   * Get the indent string based on the current settings.
   *
   * @return The indent string, either a tab or spaces based on the indent character and width.
   */
  public String getIndent() {
    final char indentChar = this.getIndentChar();
    if (indentChar == '\t') {
      return String.valueOf(indentChar);
    }

    final int indentWidth = this.getIndentWidth();
    return String.valueOf(indentChar).repeat(indentWidth);
  }
}
