package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

/**
 * Tabbed formatting indent strategy.
 *
 * <p>This strategy indents on full tabs.
 */
class TabbedIndentStrategy extends IndentStrategy {

  public static final String NAME = "tabbed";

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
  public String getStrategyName() {
    return TabbedIndentStrategy.NAME;
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
      if (parentNode != null && parentNode.is(SUB_INDENT_NODE_TYPES)) {
        subIndentNodes.add(parentNode);
      }

      // TODO: Shouldn't this go up maximally to the first INDENT_NODE_TYPE?

      subIndentNode = parentNode;
    }
    indentNodes.addAll(0, subIndentNodes);

    // Handle comments at the end of a body. The comment is added as a
    // trivia to the next token, instead of the token of the current node.
    // So the comment appears after the BODY-node.
    if (token.getOriginalValue().startsWith("#")
        && currentNode.isNot(MagikGrammar.MAGIK)
        && currentNode.getTokenLine() < token.getLine()) {
      indentNodes.add(0, currentNode);
    }

    // Prevent double indenting of the current node.
    final List<AstNode> dedupedIndentNodes =
        indentNodes.stream()
            .reduce(
                new ArrayList<AstNode>(),
                (acc, node) -> {
                  if (acc.isEmpty()) {
                    acc.add(node);
                  } else {
                    final AstNode lastNode = acc.get(acc.size() - 1);
                    if (lastNode.getTokenLine() != node.getTokenLine()) {
                      acc.add(node);
                    }
                  }

                  return acc;
                },
                (listA, listB) -> {
                  listA.addAll(listB);
                  return listA;
                });

    // Don't indent last node. We only need to do this for SIMPLE_VECTOR, ARGUMENTS,
    // and PARAMETERS, but not for BODY, VARIABLE_DEFINITION, etc.

    // But do indent it if the start token is on its own line.
    // TODO: AI generated this, so review this.
    if (currentNode.is(MagikGrammar.SIMPLE_VECTOR, MagikGrammar.ARGUMENTS, MagikGrammar.PARAMETERS)
        && token.getLine()
            > currentNode
                .getTokenLine()) { // This is not right, as we don't see if the start-token is on
      // its own line.
      dedupedIndentNodes.add(currentNode);
    }

    if (AstQuery.tokenIs(
            token,
            MagikPunctuator.BRACE_R.getValue(),
            MagikPunctuator.SQUARE_R.getValue(),
            MagikPunctuator.PAREN_R.getValue())
        && !dedupedIndentNodes.isEmpty()) {
      dedupedIndentNodes.remove(dedupedIndentNodes.size() - 1);
    }

    final int tabSize = this.options.getTabSize();
    final int indentSize = dedupedIndentNodes.size() * tabSize;
    return this.indentString(indentSize);
  }
}
