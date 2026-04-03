package nl.ramsolutions.sw.magik.languageserver.formatting;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.api.MagikKeyword;

/**
 * Keyword-based indentation heuristic for on-type formatting.
 *
 * <p>Determines the indentation for a newly-created line by inspecting the text of the previous
 * line. No AST is required, so this works even when the file has a syntax error.
 */
class NewlineIndentHelper {

  /**
   * Keywords that end a line and open a new indented block. The next line should be indented by one
   * level.
   */
  private static final Set<String> INDENT_OPENERS =
      Set.of(
          MagikKeyword.THEN.getValue(), // _then
          MagikKeyword.LOOP.getValue(), // _loop
          MagikKeyword.BLOCK.getValue(), // _block
          MagikKeyword.PROTECT.getValue(), // _protect
          MagikKeyword.PROTECTION.getValue(), // _protection
          MagikKeyword.TRY.getValue(), // _try
          MagikKeyword.WHEN.getValue(), // _when
          MagikKeyword.CATCH.getValue() // _catch
          );

  /**
   * Keywords that begin a line and close (or re-align) a block. The line itself should be de-dented
   * from the body that precedes it, and the next line follows the same level.
   */
  private static final List<String> INDENT_CLOSERS =
      List.of(
          MagikKeyword.ENDMETHOD.getValue(), // _endmethod
          MagikKeyword.ENDPROC.getValue(), // _endproc
          MagikKeyword.ENDBLOCK.getValue(), // _endblock
          MagikKeyword.ENDLOOP.getValue(), // _endloop
          MagikKeyword.ENDPROTECT.getValue(), // _endprotect
          MagikKeyword.ENDTRY.getValue(), // _endtry
          MagikKeyword.ENDCATCH.getValue(), // _endcatch
          MagikKeyword.ENDIF.getValue(), // _endif
          MagikKeyword.ELSE.getValue(), // _else
          MagikKeyword.ELIF.getValue(), // _elif
          MagikKeyword.FINALLY.getValue() // _finally
          );

  private final String indentUnit;

  /**
   * Constructor.
   *
   * @param useTabs If true, indent with a tab; otherwise use spaces.
   * @param tabSize Number of spaces per indent level when not using tabs.
   */
  NewlineIndentHelper(final boolean useTabs, final int tabSize) {
    this.indentUnit = useTabs ? "\t" : " ".repeat(tabSize);
  }

  /**
   * Compute the indentation string that should be placed at the start of the new (empty) line.
   *
   * @param previousLine The full text of the line that was just completed (before the newline).
   * @param currentIndent The leading whitespace of the previous line.
   * @return The indentation string for the new line, or {@code null} if no change is needed (the
   *     caller should leave the cursor as-is).
   */
  @CheckForNull
  String computeNewLineIndent(final String previousLine, final String currentIndent) {
    final String trimmed = previousLine.stripTrailing();

    // Check if the previous line ends with a block-opening keyword.
    for (final String opener : INDENT_OPENERS) {
      if (trimmed.equals(opener) || trimmed.endsWith(" " + opener)) {
        return currentIndent + this.indentUnit;
      }
    }

    // Check if the previous line starts with a block-closing keyword — the
    // next line resumes at the same (already de-dented) level.
    final String trimmedStart = trimmed.stripLeading();
    for (final String closer : INDENT_CLOSERS) {
      if (trimmedStart.startsWith(closer)) {
        // The current indent already reflects the de-dented level.
        return currentIndent;
      }
    }

    // No special keyword — continue at the same indent.
    return currentIndent;
  }

  /**
   * Extract the leading whitespace (spaces/tabs) from a line of source text.
   *
   * @param line Source line.
   * @return Leading whitespace, possibly empty.
   */
  static String leadingWhitespace(final String line) {
    int i = 0;
    while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
      i++;
    }
    return line.substring(0, i);
  }
}
