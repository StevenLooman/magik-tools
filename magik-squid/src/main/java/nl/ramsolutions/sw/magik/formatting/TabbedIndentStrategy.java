package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Tabbed formatting indent strategy.
 *
 * <p>This strategy indents on full tabs.
 */
class TabbedIndentStrategy extends IndentStrategy {

  private static final AstNodeType[] INDENT_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.BODY, MagikGrammar.VARIABLE_DEFINITION,
      };
  private static final AstNodeType[] SUB_INDENT_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.ASSIGNMENT_EXPRESSION,
        MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION,
        MagikGrammar.OR_EXPRESSION,
        MagikGrammar.XOR_EXPRESSION,
        MagikGrammar.AND_EXPRESSION,
        MagikGrammar.EQUALITY_EXPRESSION,
        MagikGrammar.RELATIONAL_EXPRESSION,
        MagikGrammar.ADDITIVE_EXPRESSION,
        MagikGrammar.MULTIPLICATIVE_EXPRESSION,
        MagikGrammar.EXPONENTIAL_EXPRESSION,
        MagikGrammar.SIMPLE_VECTOR,
        MagikGrammar.ARGUMENTS,
        MagikGrammar.PARAMETERS,
        MagikGrammar.METHOD_INVOCATION,
        MagikGrammar.PROCEDURE_INVOCATION,

        // TODO: Parenthesis around expressions?
      };

  TabbedIndentStrategy(final FormattingOptions options) {
    super(options);
  }

  @Override
  public String indentFor(final Token token, final AstNode currentNode) {
    // Construct the list of indent nodes.
    final List<AstNode> indentNodes = new ArrayList<>();
    AstNode indentNode = currentNode.getFirstAncestor(INDENT_NODE_TYPES);
    while (indentNode != null) {
      indentNodes.add(indentNode);

      indentNode = indentNode.getFirstAncestor(INDENT_NODE_TYPES);
    }

    // Find any sub-indent nodes, and add these to indentNodes.
    final List<AstNode> subIndentNodes = new ArrayList<>();
    AstNode subIndentNode = currentNode;
    while (subIndentNode != null) {
      final AstNode parentNode = subIndentNode.getParent();
      if (parentNode != null
          && parentNode.is(SUB_INDENT_NODE_TYPES)
          && parentNode.getToken() != subIndentNode.getToken()) {
        subIndentNodes.add(subIndentNode);
      }

      // TODO: Shouldn't this go up maximally to the first INDENT_NODE_TYPE?

      subIndentNode = parentNode;
    }
    indentNodes.addAll(0, subIndentNodes);

    // Handle comments at the end of a body. The comment is added as a
    // trivia to the next token, instead of the token of the current node.
    // So the comment appears after the node, as seen from this method.
    if (token.getOriginalValue().startsWith("#")
        && currentNode.isNot(MagikGrammar.MAGIK)
        && currentNode.getTokenLine() < token.getLine()) {
      indentNodes.add(0, currentNode);
    }

    final int tabSize = this.options.getTabSize();
    final int indentSize = indentNodes.size() * tabSize;
    return this.indentString(indentSize);
  }
}
