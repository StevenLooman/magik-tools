package nl.ramsolutions.sw.magik.formatting.next;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.Arrays;
import java.util.stream.Stream;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.formatting.FormattingOptions;

/** Walker for relative indentation in Magik code. */
public class RelativeIndentWalker extends FormattingWalker2 {

  public static final String STRATEGY_NAME = "relative";

  private static final AstNodeType[] BACKSTOP_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.MAGIK,
      };

  private static final AstNodeType[] CONSTRUCT_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.HANDLING,
        MagikGrammar.BLOCK,
        MagikGrammar.PROTECT,
        // MagikGrammar.PROTECTION,
        MagikGrammar.TRY,
        MagikGrammar.WHEN,
        MagikGrammar.CATCH,
        MagikGrammar.LOCK,
        MagikGrammar.IF,
        // MagikGrammar.ELIF,
        // MagikGrammar.ELSE,
        // MagikGrammar.FOR,
        // MagikGrammar.OVER,
        // MagikGrammar.WHILE,
        MagikGrammar.LOOP,
        // MagikGrammar.FINALLY,
        MagikGrammar.METHOD_DEFINITION,
        MagikGrammar.PROCEDURE_DEFINITION,
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
        MagikGrammar.SIMPLE_VECTOR, MagikGrammar.TUPLE, MagikGrammar.PROCEDURE_INVOCATION,
      };

  private static final AstNodeType[] METHOD_INVOCATION_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.METHOD_INVOCATION,
      };

  private static final AstNodeType[] ARGUMENTS_NODE_TYPES =
      new AstNodeType[] {
        MagikGrammar.ARGUMENTS,
        MagikGrammar.ARGUMENT,
        MagikGrammar.PARAMETERS,
        MagikGrammar.PARAMETER,
      };

  private static final AstNodeType[] NODE_TYPES;

  static {
    NODE_TYPES =
        Stream.of(
                BACKSTOP_NODE_TYPES,
                CONSTRUCT_NODE_TYPES,
                STATEMENT_NODE_TYPES,
                EXPRESSION_NODE_TYPES,
                EXPRESSION_2_NODE_TYPES,
                EXPRESSION_3_NODE_TYPES,
                METHOD_INVOCATION_NODE_TYPES,
                ARGUMENTS_NODE_TYPES)
            .flatMap(Arrays::stream)
            .toArray(AstNodeType[]::new);
  }

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
    if (interestNode.is(BACKSTOP_NODE_TYPES)) {
      this.ensureIndent(token, 0);
    } else if (interestNode.is(CONSTRUCT_NODE_TYPES)) {
      this.handleConstructNode(token, interestNode);
    } else if (interestNode.is(STATEMENT_NODE_TYPES)) {
      this.handleStatementNode(token, interestNode);
    } else if (interestNode.is(EXPRESSION_NODE_TYPES)) {
      this.handleExpressionNode(token, interestNode);
    } else if (interestNode.is(EXPRESSION_2_NODE_TYPES)) {
      this.handleExpression2Node(token, interestNode);
    } else if (interestNode.is(EXPRESSION_3_NODE_TYPES)) {
      this.handleExpression3Node(token, interestNode);
    } else if (interestNode.is(METHOD_INVOCATION_NODE_TYPES)) {
      this.handleMethodInvocationNode(token, interestNode);
    } else if (interestNode.is(ARGUMENTS_NODE_TYPES)) {
      this.handleArgumentsNode(token, interestNode);
    } else {
      throw new IllegalStateException("Unknown node type for indent: " + interestNode);
    }
  }

  private void handleConstructNode(final Token token, final AstNode node) {
    // If not first token, then align with first token.
    final Token nodeToken = node.getToken();
    if (token != nodeToken) {
      this.ensureIndentLinedUpWith(token, nodeToken);
    } else {
      // Indent from parent construct, or 0 if no parent construct.
      final AstNode parentConstruct =
          node.getFirstAncestor(RelativeIndentWalker.CONSTRUCT_NODE_TYPES);
      if (parentConstruct == null) {
        // No parent construct, so no indent.
        this.ensureIndent(token, 0);
      } else {
        // Indent from parent construct.
        final Token parentConstructToken = parentConstruct.getToken();
        this.ensureIndentFrom(token, parentConstructToken);
      }
    }
  }

  private void handleStatementNode(final Token token, final AstNode node) {
    final Token nodeToken = node.getToken();
    if (token == nodeToken) {
      // First token of statement, indent from parent construct, or 0 if no parent construct.
      final AstNode parentConstruct =
          node.getFirstAncestor(RelativeIndentWalker.CONSTRUCT_NODE_TYPES);
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
    if (token == nodeToken) {
      // First token, handle as statement.
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

  private void handleMethodInvocationNode(final Token token, final AstNode node) {
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

  private void handleArgumentsNode(final Token token, final AstNode node) {
    final AstNode argumentsOrParametersNode =
        node.is(MagikGrammar.ARGUMENT, MagikGrammar.PARAMETER)
            ? node.getFirstAncestor(
                MagikGrammar.ASSIGNMENT_ARGUMENT, MagikGrammar.ARGUMENTS, MagikGrammar.PARAMETERS)
            : node;
    final AstNode firstArgumentOrParameterNode =
        argumentsOrParametersNode.getFirstChild(MagikGrammar.ARGUMENT, MagikGrammar.PARAMETER);
    if (node == firstArgumentOrParameterNode) {
      // On new line, indent from statement.
      final AstNode parentStatementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
      final Token parentStatementToken = parentStatementNode.getToken();
      this.ensureIndentFrom(token, parentStatementToken);
    } else {
      // Not first argument or parameter, so line up with first argument or parameter.
      final Token firstArgumentOrParameterToken = firstArgumentOrParameterNode.getToken();
      this.ensureIndentLinedUpWith(token, firstArgumentOrParameterToken);
    }
  }

  private void walkLineCommentToken(final Token token, final Token commentToken) {
    // Find parent construct to indent from.
    final AstNode parentConstruct =
        this.currentNode.getFirstAncestor(RelativeIndentWalker.CONSTRUCT_NODE_TYPES);
    if (parentConstruct == null) {
      // No parent construct, so no indent.
      this.ensureIndent(commentToken, 0);
    } else {
      // Indent from parent construct.
      final Token parentConstructToken = parentConstruct.getToken();
      this.ensureIndentFrom(commentToken, parentConstructToken);
    }
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

      return;
    }

    if (indentSize != 0) {
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
