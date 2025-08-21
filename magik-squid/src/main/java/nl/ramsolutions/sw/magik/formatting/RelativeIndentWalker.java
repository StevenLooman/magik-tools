package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;

/** Walker for relative indentation in Magik code. */
public class RelativeIndentWalker extends FormattingWalker {

  public static final String STRATEGY_NAME = "relative";

  private static final AstNodeType[] BACKSTOP_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.MAGIK,
      };

  private static final AstNodeType[] HANDLING_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.HANDLING,
      };

  private static final AstNodeType[] METHOD_DEFINITION_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.METHOD_DEFINITION,
      };

  private static final AstNodeType[] CONSTRUCT_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.BLOCK,
        MagikGrammar.PROTECT,
        MagikGrammar.TRY,
        MagikGrammar.CATCH,
        MagikGrammar.LOCK,
        MagikGrammar.IF,
        MagikGrammar.PROCEDURE_DEFINITION,
      };

  private static final AstNodeType[] CONSTRUCT_2_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.LOCKING,
        MagikGrammar.PROTECTION,
        MagikGrammar.WHEN,
        MagikGrammar.ELIF,
        MagikGrammar.ELSE,
        MagikGrammar.OVER,
        MagikGrammar.LOOP,
        MagikGrammar.FINALLY,
      };

  private static final AstNodeType[] STATEMENT_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.MULTIPLE_ASSIGNMENT_STATEMENT,
        MagikGrammar.EXPRESSION_STATEMENT,
        MagikGrammar.RETURN_STATEMENT,
        MagikGrammar.EMIT_STATEMENT,
        MagikGrammar.CONTINUE_STATEMENT,
        MagikGrammar.LEAVE_STATEMENT,
        MagikGrammar.THROW_STATEMENT,
        MagikGrammar.VARIABLE_DEFINITION_STATEMENT,
        MagikGrammar.PRIMITIVE_STATEMENT,
      };

  private static final AstNodeType[] EXPRESSION_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.OR_EXPRESSION,
        MagikGrammar.XOR_EXPRESSION,
        MagikGrammar.AND_EXPRESSION,
        MagikGrammar.EQUALITY_EXPRESSION,
        MagikGrammar.RELATIONAL_EXPRESSION,
        MagikGrammar.ADDITIVE_EXPRESSION,
        MagikGrammar.MULTIPLICATIVE_EXPRESSION,
        MagikGrammar.EXPONENTIAL_EXPRESSION,
        MagikGrammar.UNARY_EXPRESSION,
      };

  private static final AstNodeType[] EXPRESSION_2_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.ASSIGNMENT_EXPRESSION, MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION,
      };

  private static final AstNodeType[] EXPRESSION_3_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.SIMPLE_VECTOR, MagikGrammar.TUPLE,
      };

  private static final AstNodeType[] INVOCATION_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.METHOD_INVOCATION, MagikGrammar.PROCEDURE_INVOCATION,
      };

  private static final AstNodeType[] PARAMETERS_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.PARAMETERS, MagikGrammar.PARAMETER,
      };

  private static final AstNodeType[] ARGUMENTS_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.ARGUMENTS, MagikGrammar.ARGUMENT,
      };

  private static final AstNodeType[] ASSIGNMENT_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.ASSIGNMENT_EXPRESSION, MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION
      };

  private static final AstNodeType[] VARIABLE_DEFINITION_STATEMENT_NODE_TYPES =
      new AstNodeType[] {MagikGrammar.VARIABLE_DEFINITION_STATEMENT};

  private static final AstNodeType[] NODE_TYPES;

  static {
    NODE_TYPES =
        Stream.of(
                RelativeIndentWalker.BACKSTOP_NODE_TYPES,
                RelativeIndentWalker.HANDLING_NODE_TYPES,
                RelativeIndentWalker.METHOD_DEFINITION_NODE_TYPES,
                RelativeIndentWalker.CONSTRUCT_NODE_TYPES,
                RelativeIndentWalker.CONSTRUCT_2_NODE_TYPES,
                RelativeIndentWalker.STATEMENT_NODE_TYPES,
                RelativeIndentWalker.EXPRESSION_NODE_TYPES,
                RelativeIndentWalker.EXPRESSION_2_NODE_TYPES,
                RelativeIndentWalker.EXPRESSION_3_NODE_TYPES,
                RelativeIndentWalker.INVOCATION_NODE_TYPES,
                RelativeIndentWalker.PARAMETERS_NODE_TYPES,
                RelativeIndentWalker.ARGUMENTS_NODE_TYPES)
            .flatMap(Arrays::stream)
            .toArray(AstNodeType[]::new);
  }

  private static final AstNodeType[] ALL_CONSTRUCT_NODE_TYPES;

  static {
    ALL_CONSTRUCT_NODE_TYPES =
        Stream.of(
                RelativeIndentWalker.METHOD_DEFINITION_NODE_TYPES,
                RelativeIndentWalker.CONSTRUCT_NODE_TYPES,
                RelativeIndentWalker.CONSTRUCT_2_NODE_TYPES)
            .flatMap(Arrays::stream)
            .toArray(AstNodeType[]::new);
  }

  private static final List<AstNodeType> OPERATOR_PRECEDENCE_NODE_TYPES =
      List.of(
          MagikGrammar.OR_EXPRESSION,
          MagikGrammar.XOR_EXPRESSION,
          MagikGrammar.AND_EXPRESSION,
          MagikGrammar.EQUALITY_EXPRESSION,
          MagikGrammar.RELATIONAL_EXPRESSION,
          MagikGrammar.ADDITIVE_EXPRESSION,
          MagikGrammar.MULTIPLICATIVE_EXPRESSION,
          MagikGrammar.EXPONENTIAL_EXPRESSION,
          MagikGrammar.UNARY_EXPRESSION);

  private AstNode currentNode;
  private Token lastToken;
  private Token lastTextToken;

  /**
   * Constructor.
   *
   * @param options Formatting options.
   * @param tokenEditor Token trivia editor.
   */
  RelativeIndentWalker(final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);
  }

  @Override
  protected void walkPreDefault(final AstNode node) {
    this.currentNode = node;
  }

  @Override
  protected void walkPostDefault(final AstNode node) {
    this.currentNode = this.currentNode.getParent();
  }

  @Override
  protected void walkTrivia(final Trivia trivia) {
    if (!trivia.getToken().getType().equals(GenericTokenType.COMMENT)) {
      return;
    }

    final Token triviaToken = trivia.getToken();
    if (!triviaToken.isOnSameLineThan(this.lastToken)) {
      this.walkLineCommentToken(this.lastTextToken, triviaToken);
    }

    this.lastTextToken = triviaToken;
  }

  @Override
  protected void walkToken(final Token token) {
    if (token.isOnSameLineThan(this.lastTextToken)) {
      // Only indent at the start of a line.
      this.lastToken = token;
      this.lastTextToken = token;
      return;
    }

    this.handleTokenNode(token, this.currentNode);

    this.lastToken = token;
    this.lastTextToken = token;
  }

  private void handleTokenNode(final Token token, final AstNode node) {
    // Determine what we are, and dispatch.
    final AstNode interestNode =
        AstQuery.getSelfOrAncestorUpTo(node, MagikGrammar.MAGIK, RelativeIndentWalker.NODE_TYPES);
    if (interestNode.is(RelativeIndentWalker.BACKSTOP_NODE_TYPES)) {
      this.ensureIndent(token, 0);
    } else if (interestNode.is(RelativeIndentWalker.HANDLING_NODE_TYPES)) {
      this.handleHandlingNode(token, interestNode);
    } else if (interestNode.is(RelativeIndentWalker.METHOD_DEFINITION_NODE_TYPES)) {
      this.handleMethodDefinitionNode(token, interestNode);
    } else if (interestNode.is(RelativeIndentWalker.CONSTRUCT_NODE_TYPES)) {
      this.handleConstructNode(token, interestNode);
    } else if (interestNode.is(RelativeIndentWalker.CONSTRUCT_2_NODE_TYPES)) {
      this.handleConstruct2Node(token, interestNode);
    } else if (interestNode.is(RelativeIndentWalker.STATEMENT_NODE_TYPES)) {
      this.handleStatementNode(token, interestNode);
    } else if (interestNode.is(RelativeIndentWalker.EXPRESSION_NODE_TYPES)) {
      this.handleExpressionNode(token, interestNode);
    } else if (interestNode.is(RelativeIndentWalker.EXPRESSION_2_NODE_TYPES)) {
      this.handleExpression2Node(token, interestNode);
    } else if (interestNode.is(RelativeIndentWalker.EXPRESSION_3_NODE_TYPES)) {
      this.handleExpression3Node(token, interestNode);
    } else if (interestNode.is(RelativeIndentWalker.INVOCATION_NODE_TYPES)) {
      this.handleInvocationNode(token, interestNode);
    } else if (interestNode.is(RelativeIndentWalker.PARAMETERS_NODE_TYPES)) {
      this.handleParametersNode(token, interestNode);
    } else if (interestNode.is(RelativeIndentWalker.ARGUMENTS_NODE_TYPES)) {
      this.handleArgumentsNode(token, interestNode);
    } else {
      throw new IllegalStateException("Unknown node type for indent: " + interestNode);
    }
  }

  private void handleHandlingNode(final Token token, final AstNode node) {
    final Token nodeToken = node.getToken();
    if (token != nodeToken) {
      this.ensureIndentFrom(token, nodeToken);
    } else {
      final AstNode parentThing =
          AstQuery.getFirstAncestor(
              node,
              RelativeIndentWalker.CONSTRUCT_NODE_TYPES,
              RelativeIndentWalker.CONSTRUCT_2_NODE_TYPES);
      if (parentThing == null) {
        // No parent construct, so no indent (should never happen, but for safety.)
        this.ensureIndent(token, 0);
      } else {
        // Indent from first token of parent construct.
        final Token parentToken = parentThing.getToken();
        this.ensureIndentFrom(token, parentToken);
      }
    }
  }

  private void handleMethodDefinitionNode(final Token token, final AstNode node) {
    this.ensureIndent(token, 0);
  }

  private void handleConstructNode(final Token token, final AstNode node) {
    final Token nodeToken = node.getToken();
    if (token != nodeToken) {
      // If not first token, then align with first token.
      this.ensureIndentLinedUpWith(token, nodeToken);
    } else {
      // Indent from parent construct, or 0 if no parent construct.
      // Test what we are part of, and handle accordingly.
      final AstNode parentThing =
          AstQuery.getFirstAncestor(
              node,
              RelativeIndentWalker.ARGUMENTS_NODE_TYPES,
              RelativeIndentWalker.CONSTRUCT_NODE_TYPES,
              RelativeIndentWalker.ASSIGNMENT_NODE_TYPES,
              RelativeIndentWalker.VARIABLE_DEFINITION_STATEMENT_NODE_TYPES);
      if (parentThing == null) {
        // No parent construct, so no indent.
        this.ensureIndent(token, 0);
      } else if (parentThing.is(RelativeIndentWalker.ARGUMENTS_NODE_TYPES)) {
        this.handleArgumentsNode(token, parentThing);
      } else if (parentThing.is(RelativeIndentWalker.ASSIGNMENT_NODE_TYPES)
          || parentThing.is(RelativeIndentWalker.VARIABLE_DEFINITION_STATEMENT_NODE_TYPES)) {
        this.handleStatementNode(token, parentThing);
      } else if (parentThing.is(RelativeIndentWalker.CONSTRUCT_NODE_TYPES)) {
        // Handle as statement.
        this.handleStatementNode(token, node);
      } else {
        throw new IllegalStateException("Unknown parent thing type for indent: " + parentThing);
      }
    }
  }

  private void handleConstruct2Node(final Token token, final AstNode node) {
    // TODO: Do the special handling for OVER, and LOOP constructs in expr2.
    // Special handling for OVER, and LOOP constructs, which can stand or their own.
    final AstNode parentNode = node.getParent();
    if (node.is(MagikGrammar.OVER) || node.is(MagikGrammar.LOOP)) {
      final AstNode parentParentNode = parentNode.getParent();
      if (AstQuery.tokenIs(token, MagikKeyword.ENDLOOP.getValue())) {
        // Line up with `_loop`.
        final Token tokenNode = node.getToken();
        this.ensureIndentLinedUpWith(token, tokenNode);
      } else if (parentParentNode.is(MagikGrammar.FOR)) { // token is `_loop`.
        // Line up with `_for`.
        final Token parentParentToken = parentParentNode.getToken();
        this.ensureIndentLinedUpWith(token, parentParentToken);
      } else if (parentNode.is(MagikGrammar.FOR)) { // token is `_over`.
        // Line up with `_for`.
        final Token parentToken = parentNode.getToken();
        this.ensureIndentLinedUpWith(token, parentToken);
      } else if (parentNode.is(MagikGrammar.WHILE)) { // token is `_loop`
        // Line up with `_for`/`_while`.
        final Token parentToken = parentNode.getToken();
        this.ensureIndentLinedUpWith(token, parentToken);
      } else if (parentNode.is(MagikGrammar.OVER)) { // token is `_loop`.
        // Line up with `_over`.
        final Token parentToken = parentNode.getToken();
        this.ensureIndentLinedUpWith(token, parentToken);
      } else if (parentNode.is(MagikGrammar.ATOM)) {
        // Handle as expr2.
        final AstNode interestParentNode =
            AstQuery.getFirstAncestor(
                parentNode,
                RelativeIndentWalker.EXPRESSION_NODE_TYPES,
                RelativeIndentWalker.EXPRESSION_2_NODE_TYPES,
                RelativeIndentWalker.VARIABLE_DEFINITION_STATEMENT_NODE_TYPES);
        if (interestParentNode != null) {
          this.handleExpression2Node(token, interestParentNode);
        } else {
          // Handle as statement.
          this.handleStatementNode(token, node);
        }
      } else {
        throw new IllegalStateException("Unknown parent node type: " + parentNode.getType());
      }
    } else {
      // Align with first token of (parent) construct.
      final Token parentToken = parentNode.getToken();
      this.ensureIndentLinedUpWith(token, parentToken);
    }
  }

  private void handleStatementNode(final Token token, final AstNode node) {
    final Token nodeToken = node.getToken();
    if (token == nodeToken) {
      // First token of statement, indent from parent construct, or 0 if no parent construct.
      final AstNode parentConstruct =
          AstQuery.getFirstAncestor(
              node,
              RelativeIndentWalker.METHOD_DEFINITION_NODE_TYPES,
              RelativeIndentWalker.CONSTRUCT_NODE_TYPES,
              RelativeIndentWalker.CONSTRUCT_2_NODE_TYPES);
      if (parentConstruct == null) {
        // No parent construct, so no indent.
        this.ensureIndent(token, 0);
      } else {
        // Indent from parent construct.
        final Token parentConstructToken = parentConstruct.getToken();
        this.ensureIndentFrom(token, parentConstructToken);
      }
    } else {
      // Not first token, so indent from first token of statement.
      this.ensureIndentFrom(token, nodeToken);
    }
  }

  private void handleExpressionNode(final Token token, final AstNode node) {
    final Token nodeToken = node.getToken();
    final AstNode parentNode = node.getParent();
    if (parentNode.is(RelativeIndentWalker.EXPRESSION_NODE_TYPES)) {
      // We need operator precedence to determine if we should indent from parentNode or line up.
      final AstNodeType nodeType = node.getType();
      final int precedence = OPERATOR_PRECEDENCE_NODE_TYPES.indexOf(nodeType);
      final AstNodeType parentNodeType = parentNode.getType();
      final int parentPrecedence = OPERATOR_PRECEDENCE_NODE_TYPES.indexOf(parentNodeType);
      final Token parentToken = parentNode.getToken();
      if (precedence > parentPrecedence && nodeToken.getLine() == parentToken.getLine()) {
        // Line up with first token.
        this.ensureIndentLinedUpWith(token, nodeToken);
      } else {
        this.ensureIndentLinedUpWith(token, parentToken);
      }
    } else if (token == nodeToken) {
      this.handleStatementNode(token, node);
    } else {
      // Line up with first token.
      this.ensureIndentLinedUpWith(token, nodeToken);
    }
  }

  private void handleExpression2Node(final Token token, final AstNode node) {
    // Indent from parent statement.
    final Token nodeToken = node.getToken();
    if (token == nodeToken) {
      // Handle as statement.
      // TODO: Will this work properly with nested assignments etc?
      this.handleStatementNode(token, node);
    } else {
      // TODO: Should this always indent from parent statement? Not from any other node type?
      final AstNode parentStatementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
      final Token parentStatementToken = parentStatementNode.getToken();
      this.ensureIndentFrom(token, parentStatementToken);
    }
  }

  private void handleExpression3Node(final Token token, final AstNode node) {
    final Token firstNodeToken = node.getToken();
    final Token lastNodeToken = node.getLastToken();
    if (token == firstNodeToken) {
      // Find the thing to indent from/line up with.
      final AstNode parentNode = node.getParent();
      this.handleTokenNode(token, parentNode);
    } else if (token == lastNodeToken) {
      // Last token, so line up with first token.
      this.ensureIndentLinedUpWith(token, firstNodeToken);
    } else {
      // Not first or last token, so indent from first token.
      this.ensureIndentFrom(token, firstNodeToken);
    }
  }

  private void handleInvocationNode(final Token token, final AstNode node) {
    final Token lastNodeToken = node.getLastToken();
    final AstNode indentFromNode =
        node.getFirstAncestor(MagikGrammar.STATEMENT, MagikGrammar.EXPRESSION);
    final Token indentFromToken = indentFromNode.getToken();
    // token == firstNodeToken can never happen,
    // as the `.` has to be on the same line as the object it is being called on.
    if (token == lastNodeToken) {
      // Indent from statement.
      this.ensureIndentLinedUpWith(token, indentFromToken);
    } else {
      // Method name, indent from statement.
      this.ensureIndentFrom(token, indentFromToken);
    }
  }

  private void handleParametersNode(final Token token, final AstNode node) {
    final AstNode parametersNode =
        node.is(MagikGrammar.PARAMETER)
            ? node.getFirstAncestor(MagikGrammar.ASSIGNMENT_PARAMETER, MagikGrammar.PARAMETERS)
            : node;
    final AstNode firstParameterNode = parametersNode.getFirstChild(MagikGrammar.PARAMETER);
    if (node == firstParameterNode) {
      // Indent from parent invocation, first token on that line.
      final AstNode invocationNode =
          node.getFirstAncestor(MagikGrammar.METHOD_INVOCATION, MagikGrammar.PROCEDURE_INVOCATION);
      final Token invocationToken = invocationNode.getToken();
      final Token invocationNodeFirstLineToken = this.getFirstTextTokenOnLine(invocationToken);
      this.ensureIndentFrom(token, invocationNodeFirstLineToken);
    } else {
      // Not first parameter, so line up with first parameter.
      final Token firstParameterToken = firstParameterNode.getToken();
      this.ensureIndentLinedUpWith(token, firstParameterToken);
    }
  }

  private void handleArgumentsNode(final Token token, final AstNode node) {
    final AstNode argumentsNode =
        node.is(MagikGrammar.ARGUMENT, MagikGrammar.PARAMETER)
            ? node.getFirstAncestor(
                MagikGrammar.ASSIGNMENT_ARGUMENT, MagikGrammar.ARGUMENTS, MagikGrammar.PARAMETERS)
            : node;
    final AstNode firstArgumentNode =
        argumentsNode.getFirstChild(MagikGrammar.ARGUMENT, MagikGrammar.PARAMETER);
    if (node == firstArgumentNode) {
      // Indent from parent invocation, first token on that line.
      final AstNode invocationNode =
          node.getFirstAncestor(MagikGrammar.METHOD_INVOCATION, MagikGrammar.PROCEDURE_INVOCATION);
      final Token invocationToken = invocationNode.getToken();
      final Token invocationNodeFirstLineToken = this.getFirstTextTokenOnLine(invocationToken);
      this.ensureIndentFrom(token, invocationNodeFirstLineToken);
    } else {
      // Not first argument, so line up with first argument.
      final Token firstArgumentToken = firstArgumentNode.getToken();
      this.ensureIndentLinedUpWith(token, firstArgumentToken);
    }
  }

  private void walkLineCommentToken(final Token token, final Token commentToken) {
    final AstNode usedNode =
        commentToken.getLine() < this.currentNode.getToken().getLine()
            ? this.currentNode.getParent()
            : this.currentNode;

    // Find parent construct to indent from.
    final AstNode parentConstruct =
        AstQuery.getSelfOrAncestorUpTo(
            usedNode, MagikGrammar.MAGIK, RelativeIndentWalker.ALL_CONSTRUCT_NODE_TYPES);
    if (parentConstruct == null) {
      // No parent construct, so no indent.
      this.ensureIndent(commentToken, 0);
    } else {
      // Indent from parent construct.
      final Token parentConstructToken = parentConstruct.getToken();
      this.ensureIndentFrom(commentToken, parentConstructToken);
    }
  }

  private Token getFirstTextTokenOnLine(final Token invocationToken) {
    Token currentToken = invocationToken;
    Token lastTextToken = invocationToken;
    while (currentToken != null) {
      final Token tokenBefore = this.getTokenBeforeOnSameLine(currentToken);
      if (tokenBefore == null) {
        // No more tokens before.
        break;
      }

      final TokenType tokenBeforeType = tokenBefore.getType();
      if (!tokenBeforeType.equals(GenericTokenType.WHITESPACE)) {
        lastTextToken = tokenBefore;
      }

      currentToken = tokenBefore;
    }

    return lastTextToken;
  }

  /**
   * Indent from the reference token.
   *
   * @param token The token to indent.
   * @param referenceToken The token to indent from.
   */
  private void ensureIndentFrom(final Token token, final Token referenceToken) {
    final int referenceColumn = this.getIndentSize(referenceToken);
    final int indentSize = referenceColumn + this.getOptions().getTabSize();
    this.ensureIndent(token, indentSize);
  }

  /**
   * Ensures that the given token is indented to the same column as the reference token.
   *
   * @param token The token to line up.
   * @param referenceToken The token to line up with.
   */
  private void ensureIndentLinedUpWith(final Token token, final Token referenceToken) {
    if (token == referenceToken) {
      throw new IllegalArgumentException("Token and reference token are the same");
    }

    final int indentSize = this.getIndentSize(referenceToken);
    this.ensureIndent(token, indentSize);
  }

  /**
   * Calculate the indent size based on text before the referenceToken and configured tab size.
   *
   * @param referenceToken The token to calculate the indent size from.
   */
  private int getIndentSize(final Token referenceToken) {
    // Calculate the indent size based on text before the referenceToken and configured tab size.
    // Determine all the text before the referenceToken on the same line.
    final StringBuilder stringBuilder = new StringBuilder();
    Token tokenBefore = this.getTokenBeforeOnSameLine(referenceToken);
    while (tokenBefore != null) {
      final String tokenBeforeValue = tokenBefore.getOriginalValue();
      stringBuilder.append(tokenBeforeValue);
      tokenBefore = this.getTokenBeforeOnSameLine(tokenBefore);
    }

    // Note: This that assumes tabs are only used at the start of each line.
    final int tabSize = this.getOptions().getTabSize();
    final String untabbedString = " ".repeat(tabSize);
    return stringBuilder.toString().replaceAll("\t", untabbedString).length();
  }

  private void ensureIndent(final Token token, final int indentSize) {
    final String indentString = this.indentString(indentSize);
    final Token tokenBefore = this.getTokenBeforeOnSameLine(token);
    if (tokenBefore != null && tokenBefore.getType().equals(GenericTokenType.WHITESPACE)) {
      if (tokenBefore.getValue().equals(indentString)) {
        // Already has the correct indent, nothing to do.
      } else if (indentSize == 0) {
        // No indent, so remove the whitespace.
        this.removeWhitespaceToken(tokenBefore);
      } else {
        // Has whitespace, but not the correct indent, so we need to replace it.
        this.setTokenOriginalValue(tokenBefore, indentString);
      }
    } else if (indentSize != 0) {
      // No indent, so remove any existing whitespace.
      this.ensureWhitespaceBefore(token, indentString);
    }
  }

  /**
   * Get the indent string for a given size.
   *
   * @param indentSize Number of white spaces.
   * @return Indent string.
   */
  private String indentString(final int indentSize) {
    if (indentSize == 0) {
      return "";
    }

    final int tabSize = this.getOptions().getTabSize();
    final String tabText = this.getOptions().isInsertSpaces() ? " ".repeat(tabSize) : "\t";
    final int indent1 = indentSize / tabSize;
    final int indent2 = indentSize % tabSize;
    return tabText.repeat(indent1) + " ".repeat(indent2);
  }
}
