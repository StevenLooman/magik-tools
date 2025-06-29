package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

/**
 * Relative indent strategy.
 *
 * <p>This strategy indents relatively.
 */
class RelativeIndentStrategy extends IndentStrategy {

  public static final String NAME = "relative";

  private static final AstNodeType[] INDENT_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.BODY,
        MagikGrammar.VARIABLE_DEFINITION,
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
        MagikGrammar.ARGUMENT,
        MagikGrammar.PARAMETER,
        MagikGrammar.METHOD_INVOCATION,
        MagikGrammar.PROCEDURE_INVOCATION,
        MagikGrammar.IF,

        // TODO: Parenthesis around expressions?
      };

  private TokenColumnTracker tokenColumnTracker;

  RelativeIndentStrategy(
      final FormattingOptions options, final TokenColumnTracker tokenColumnTracker) {
    super(options);
    this.tokenColumnTracker = tokenColumnTracker;
  }

  RelativeIndentStrategy(final FormattingOptions options) {
    super(options);
  }

  @Override
  public void setTokenColumnTracker(final TokenColumnTracker tokenColumnTracker) {
    this.tokenColumnTracker = tokenColumnTracker;
  }

  @Override
  public String getStrategyName() {
    return RelativeIndentStrategy.NAME;
  }

  @Override
  public String indentFor(final Token token, final AstNode currentNode) {
    final int tabSize = this.options.getTabSize();

    final AstNode parentIndentNode = this.findParentIndentNode(token, currentNode);
    if (parentIndentNode == null) {
      // No indent node found, must be at top level.
      return this.indentString(0);
    }

    final Token parentIndentToken = parentIndentNode.getToken();
    if (parentIndentNode.is(
            // Expressions.
            MagikGrammar.AND_EXPRESSION,
            // Construects.
            MagikGrammar.IF)
        && parentIndentToken != token) {
      // Indent at expression start token.
      final int tokenColumn = this.tokenColumnTracker.getNewColumn(parentIndentToken);
      return this.indentString(tokenColumn);
    }

    if (parentIndentNode.is(MagikGrammar.ARGUMENT)) {
      // If the first argument starts on the same line as the invocation,
      // line up with the first argument.
      // If the first arguments starts on a new line, indent relative to the
      // invocation.
      final AstNode argumentsNode = parentIndentNode.getFirstAncestor(MagikGrammar.ARGUMENTS);
      final AstNode firstArgumentNode = argumentsNode.getFirstChild(MagikGrammar.ARGUMENT);
      if (firstArgumentNode == parentIndentNode) {
        // We are the first argument and on a new line, indent relative to the invocation.
        final AstNode statementNode = parentIndentNode.getFirstAncestor(MagikGrammar.STATEMENT);
        final Token statementToken = statementNode.getToken();
        final int tokenColumn = this.tokenColumnTracker.getNewColumn(statementToken);
        return this.indentString(tokenColumn + tabSize);
      } else {
        // We are not the first argument, so line up with the first argument.
        final Token firstArgumentToken = firstArgumentNode.getToken();
        final int tokenColumn = this.tokenColumnTracker.getNewColumn(firstArgumentToken);
        return this.indentString(tokenColumn);
      }
    }

    if (parentIndentNode.is(MagikGrammar.BODY)) {
      final AstNode bodyParentNode = parentIndentNode.getParent();
      final Token bodyParentToken = bodyParentNode.getToken();
      final int tokenColumn = this.tokenColumnTracker.getNewColumn(bodyParentToken);
      return this.indentString(tokenColumn + tabSize);
    }

    // Indent relatively to the parent indent node.
    final int parentIndentTokenColumn = this.tokenColumnTracker.getNewColumn(parentIndentToken);
    return this.indentString(parentIndentTokenColumn + tabSize).concat(NAME);
  }

  @CheckForNull
  private AstNode findParentIndentNode(final Token token, final AstNode currentNode) {
    // Handle comments at the end of a body. The comment is added as a
    // trivia to the next token, instead of the token of the current node.
    // So the comment appears after the BODY-node.
    if (token.getOriginalValue().startsWith("#")
        && currentNode.isNot(MagikGrammar.MAGIK)
        && currentNode.getTokenLine() < token.getLine()) {
      return currentNode;
    }

    // TODO: _when, _protection, loop?, end*, ...

    if (AstQuery.tokenIs(
        token,
        MagikKeyword.THEN.getValue(),
        MagikKeyword.ENDIF.getValue(),
        MagikKeyword.ELSE.getValue(),
        MagikKeyword.ELIF.getValue())) {
      return AstQuery.getSelfOrAncestorUpTo(currentNode, MagikGrammar.IF, MagikGrammar.IF);
    }

    if (AstQuery.tokenIs(token, MagikPunctuator.BRACE_R.getValue())) {
      return currentNode;
    }

    final AstNode firstAncestor = currentNode.getFirstAncestor(INDENT_NODE_TYPES);
    if (firstAncestor == null) {
      return null;
    }

    // if (firstAncestor.is(MagikGrammar.BODY)
    //     // || firstAncestor.is(MagikGrammar.VARIABLE_DEFINITION)
    //     // || firstAncestor.is(MagikGrammar.ASSIGNMENT_EXPRESSION)
    //     // || firstAncestor.is(MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION)
    //     ) {
    //   // Indent relative to the body or variable definition.
    //   return firstAncestor.getParent();
    // }

    return firstAncestor;
  }
}
