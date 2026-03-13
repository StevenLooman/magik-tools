package nl.ramsolutions.sw.magik.languageserver.codeactions;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Shared utilities for extract-to-method/proc code action providers. */
class ExtractUtils {

  private ExtractUtils() {}

  /**
   * Node types that are unconditionally disallowed inside a selection for extraction. {@link
   * MagikGrammar#LEAVE_STATEMENT} and {@link MagikGrammar#CONTINUE_STATEMENT} are handled
   * conditionally by {@link #containsDisallowedNodes}.
   */
  private static final AstNodeType[] DISALLOWED_NODE_TYPES = {
    MagikGrammar.RETURN_STATEMENT,
    MagikGrammar.THROW_STATEMENT,
    MagikGrammar.EMIT_STATEMENT,
    MagikGrammar.LOOPBODY,
  };

  /**
   * Returns {@code true} if any selected statement contains a node that would be semantically
   * invalid inside an extracted method.
   *
   * <p>{@code _leave} and {@code _continue} are only disallowed when they refer to a loop that is
   * <em>outside</em> the selected statement (i.e., they would be orphaned in the extracted method).
   * If the enclosing {@link MagikGrammar#LOOP} is itself within the selected statement, the
   * construct is valid.
   */
  static boolean containsDisallowedNodes(final List<AstNode> statements) {
    for (final AstNode stmt : statements) {
      for (final AstNode node : AstQuery.dfs(stmt).toList()) {
        if (node.is(DISALLOWED_NODE_TYPES)) {
          return true;
        }
        if (node.is(MagikGrammar.LEAVE_STATEMENT, MagikGrammar.CONTINUE_STATEMENT)
            && !hasEnclosingLoopWithin(node, stmt)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if {@code node} has an ancestor that is a {@link MagikGrammar#LOOP} and
   * that ancestor is a strict descendant of {@code stmtRoot} (i.e., within the selection).
   */
  private static boolean hasEnclosingLoopWithin(final AstNode node, final AstNode stmtRoot) {
    AstNode ancestor = node.getParent();
    while (ancestor != null && ancestor != stmtRoot) {
      if (ancestor.is(MagikGrammar.LOOP)) {
        return true;
      }
      ancestor = ancestor.getParent();
    }
    return false;
  }

  /**
   * Prefix every line in {@code text} with {@code prefix}.
   *
   * <p>The input is assumed to end with a newline; the result also ends with a single newline.
   *
   * @param text The text whose lines should be prefixed.
   * @param prefix The string to prepend to each line.
   * @return The re-indented text.
   */
  static String prefixLines(final String text, final String prefix) {
    return text.lines().map(l -> prefix + l).collect(Collectors.joining("\n")) + "\n";
  }

  /**
   * Get the leading whitespace of the given (1-based) source line.
   *
   * @param magikFile The Magik file.
   * @param line The 1-based line number.
   * @return The leading whitespace string, or {@code ""} if the line does not exist.
   */
  static String getLineLeadingWhitespace(final MagikTypedFile magikFile, final int line) {
    final String source = magikFile.getSource();
    if (source == null) {
      return "";
    }
    final String[] lines = source.split("\n", -1);
    if (line <= 0 || line > lines.length) {
      return "";
    }
    final String srcLine = lines[line - 1];
    int i = 0;
    while (i < srcLine.length() && (srcLine.charAt(i) == ' ' || srcLine.charAt(i) == '\t')) {
      i++;
    }
    return srcLine.substring(0, i);
  }

  /**
   * Get the pragma line for an extracted {@code _private} method, read directly from the source
   * line that immediately precedes the {@code _method} keyword.
   *
   * <p>Using the raw source line (rather than the parsed PRAGMA AST node) is more robust because
   * some common pragma forms — e.g. {@code topic={}} with an empty brace list — cannot be parsed by
   * the grammar rule and therefore never appear as a PRAGMA sibling in the AST.
   *
   * <p>If the classify_level is {@code basic} or {@code advanced} it is downgraded to {@code
   * restricted}, since a private method must not be more accessible than restricted.
   *
   * @param magikFile The Magik file containing the source method.
   * @param methodDefNode The METHOD_DEFINITION node of the source method.
   * @return The (possibly adjusted) pragma text, or {@code null} if the source method has no pragma
   *     on the immediately preceding line.
   */
  @CheckForNull
  static String getPragmaLineForPrivateMethod(
      final MagikTypedFile magikFile, final AstNode methodDefNode) {
    final String source = magikFile.getSource();
    if (source == null) {
      return null;
    }
    final String[] lines = source.split("\n", -1);
    // methodDefNode.getToken().getLine() is 1-based; the pragma is on the line before _method.
    final int methodLine = methodDefNode.getToken().getLine(); // 1-based
    final int pragmaIdx = methodLine - 2; // 0-based index of the line before _method
    if (pragmaIdx < 0 || pragmaIdx >= lines.length) {
      return null;
    }
    // Strip trailing \r to handle CRLF files.
    final String lineText = lines[pragmaIdx].stripTrailing();
    if (!lineText.strip().toLowerCase().startsWith("_pragma(")) {
      return null;
    }
    // Private methods must not be more accessible than restricted.
    return lineText.replaceAll(
        "classify_level\\s*=\\s*(?:basic|advanced)\\b", "classify_level=restricted");
  }
}
