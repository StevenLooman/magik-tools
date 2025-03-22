package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Default formatting indent strategy. */
class DefaultIndentStrategy extends FormattingIndentStrategy {

  DefaultIndentStrategy(final FormattingOptions options) {
    super(options);
  }

  @Override
  public String indentFor(final Token token, final AstNode currentNode) {
    // Determine depth of this.currentNode.
    final AstNodeType[] indentNodeTypes =
        new AstNodeType[] {
          MagikGrammar.BODY, MagikGrammar.VARIABLE_DEFINITION,
        };
    final List<AstNode> indentNodes = new ArrayList<>();
    AstNode indentNode = currentNode.getFirstAncestor(indentNodeTypes);
    while (indentNode != null) {
      indentNodes.add(indentNode);

      indentNode = indentNode.getFirstAncestor(indentNodeTypes);
    }

    // If inside expression (thus not the first node of the expression), also increase indent.
    final AstNodeType[] subIndentNodeTypes =
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
    final List<AstNode> subIndentNodes = new ArrayList<>();
    AstNode subIndentNode = currentNode;
    while (subIndentNode != null) {
      final AstNode parentNode = subIndentNode.getParent();
      if (parentNode != null
          && parentNode.is(subIndentNodeTypes)
          && parentNode.getToken() != subIndentNode.getToken()) {
        subIndentNodes.add(subIndentNode);
      }

      subIndentNode = parentNode;
    }
    indentNodes.addAll(0, subIndentNodes);

    // Handle comments at the end of a body. The comments is added as a
    // trivia to the next token, instead of the token of the current node.
    // So the comment appears after the node, as seen from this method.
    if (token.getOriginalValue().startsWith("#")
        && currentNode.isNot(MagikGrammar.MAGIK)
        && currentNode.getTokenLine() < token.getLine()) {
      indentNodes.add(0, currentNode);
    }

    // TODO: Recurse for each indent-node, starting from parent. Then, add n-indent to that.
    // This is for the emacs endenting rules!
    // For example, `10` indents n(=4) from `{`, not based node structure:
    // _local x << {
    //                 10
    //             }
    // So should we indent based on node, and not token?

    final int tabSize = this.options.getTabSize();
    final String tabText = this.options.isInsertSpaces() ? " ".repeat(tabSize) : "\t";
    final int indentSize = indentNodes.size() * tabSize;
    if (indentSize == 0) {
      return "";
    }

    final int indent1 = indentSize / tabSize;
    final int indent2 = indentSize % tabSize;
    return tabText.repeat(indent1) + " ".repeat(indent2);
  }
}
