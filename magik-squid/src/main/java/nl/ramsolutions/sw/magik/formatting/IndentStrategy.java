package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.AstQuery;

abstract class IndentStrategy extends FormattingStategy {

  /**
   * Constructor.
   *
   * @param options Options.
   */
  IndentStrategy(final FormattingOptions options) {
    super(options);
  }

  @CheckForNull
  TextEdit ensureIndenting(final Token token, final AstNode currentNode) {
    final String indentText = this.indentFor(token, currentNode);
    final String reason = "improper indenting";
    if (!AstQuery.tokenIs(this.lastToken, GenericTokenType.WHITESPACE)) {
      if (!indentText.isEmpty()) {
        return this.insertBeforeToken(token, indentText, reason);
      }
    } else if (!this.lastToken.getOriginalValue().equals(indentText)) {
      return this.editToken(this.lastToken, indentText, reason);
    }

    return null;
  }

  /**
   * Get the indent for a token.
   *
   * @param lastToken Last token.
   * @param token Token to indent.
   * @param currentNode The current AstNode.
   * @return Indent string.
   */
  abstract String indentFor(final Token token, final AstNode currentNode);

  /**
   * Get the indent string for a given size.
   *
   * @param indentSize Number of white spaces.
   * @return Indent string.
   */
  String indentString(final int indentSize) {
    final int tabSize = this.options.getTabSize();
    final String tabText = this.options.isInsertSpaces() ? " ".repeat(tabSize) : "\t";
    if (indentSize == 0) {
      return "";
    }

    final int indent1 = indentSize / tabSize;
    final int indent2 = indentSize % tabSize;
    return tabText.repeat(indent1) + " ".repeat(indent2);
  }
}
