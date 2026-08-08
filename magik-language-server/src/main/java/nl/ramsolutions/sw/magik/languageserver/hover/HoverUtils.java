package nl.ramsolutions.sw.magik.languageserver.hover;

import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Shared helpers for hover modules. */
final class HoverUtils {

  static final String NBSP_NBSP = "&nbsp;&nbsp;";
  static final String SECTION_END = "\n\n---\n\n";

  private HoverUtils() {}

  /**
   * Format a {@link TypeString} for use in markdown.
   *
   * @param typeStr Type string.
   * @return Formatted type string.
   */
  static String formatTypeString(final TypeString typeStr) {
    final String fullString = typeStr.getFullString();
    return HoverUtils.formatTypeString(fullString);
  }

  /**
   * Format a type name for use in markdown, by replacing the generic brackets which markdown would
   * otherwise swallow.
   *
   * @param typeStr Type name.
   * @return Formatted type name.
   */
  static String formatTypeString(final String typeStr) {
    return typeStr.replace("<", "[").replace(">", "]");
  }

  /**
   * Convert a doc to markdown, by joining its lines into paragraphs.
   *
   * @param doc Doc.
   * @return Markdown.
   */
  static String docToMarkdown(final String doc) {
    return doc.lines().map(String::trim).collect(Collectors.joining("\n\n"));
  }
}
