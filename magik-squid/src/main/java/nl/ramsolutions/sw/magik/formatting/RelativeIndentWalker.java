package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

/**
 * A formatting walker that handles relative indentation for Magik code.
 *
 * <p>This walker uses a strategy pattern to determine indentation for different AST node types.
 * Each token is indented based on its parent node type and the applicable indentation strategy.
 *
 * <h2>Strategy Pattern</h2>
 *
 * <p>The walker defines an {@link IndentStrategy} enum with the following strategies:
 *
 * <ul>
 *   <li>{@code TOP_LEVEL} - No indentation (column 0). Used for the top-level MAGIK node.
 *   <li>{@code IF_KEYWORD_OR_BODY} - Special handling for IF constructs where keywords (_then,
 *       _else, _elif, _endif) align with _if, and body content is indented.
 *   <li>{@code BODY_FROM_PARENT_START} - Body content is indented one level from the parent's start
 *       token. Used for methods, procedures, blocks, try, catch, lock.
 *   <li>{@code ARGUMENT_LIST} - Arguments/parameters align with the first argument. Used for method
 *       and procedure arguments.
 *   <li>{@code ALIGN_TO_FIRST_CHILD} - Continuation lines align with the first child token. Used
 *       for AND expressions.
 *   <li>{@code ASSIGNMENT_RIGHT_HAND_SIDE} - Right-hand side of assignments is indented from the
 *       start of the line containing the assignment.
 *   <li>{@code COLLECTION_BODY} - Collection elements are indented from the opening brace; closing
 *       brace aligns with the line start.
 *   <li>{@code INVOCATION_CHAIN} - Method invocation chains are indented from the statement start.
 *       Supports fluent interfaces.
 *   <li>{@code CLAUSE_FROM_PARENT} - Clause keywords (_loop, _protection, _when, etc.) align with
 *       their parent clause. Body is indented from the clause keyword.
 *   <li>{@code EXPRESSION_PRECEDENCE} - Binary expressions respect operator precedence for
 *       alignment.
 * </ul>
 *
 * <h2>Node Type to Strategy Mapping</h2>
 *
 * <p>The {@link #STRATEGY_BY_NODE_TYPE} map defines which strategy applies to each AST node type.
 * When indenting a token, the walker finds the nearest ancestor node that has a strategy mapping
 * and applies that strategy.
 *
 * <h2>Example Indentation</h2>
 *
 * <pre>{@code
 * _method my_class.my_method(arg1,    # BODY_FROM_PARENT_START
 *                            arg2)    # ARGUMENT_LIST - aligns with arg1
 *     _local result <<                # indented from _method
 *         _if condition               # ASSIGNMENT_RIGHT_HAND_SIDE
 *         _then                       # IF_KEYWORD_OR_BODY - aligns with _if
 *             value1                  # indented from _if
 *         _else                       # aligns with _if
 *             value2                  # indented from _if
 *         _endif                      # aligns with _if
 *     _return result
 * _endmethod                          # aligns with _method
 * }</pre>
 *
 * @see FormattingWalker
 * @see IndentStrategy
 */
public class RelativeIndentWalker extends FormattingWalker {

  /** Strategy name for configuration and identification. */
  public static final String STRATEGY_NAME = "relative";

  /**
   * Indentation strategies for different AST constructs.
   *
   * <p>Each strategy defines how tokens within a particular AST node type should be indented
   * relative to their parent or sibling tokens.
   */
  private enum IndentStrategy {
    /** No indentation - tokens at column 0. Used for top-level constructs. */
    TOP_LEVEL,

    /**
     * Body content is indented one tab stop from the parent's start token. Structural keywords
     * (like _then/_else/_endif for IF, or _endmethod for methods) align with the start keyword.
     * Used for: _method, _proc, _block, _if, _try, _catch, _lock.
     */
    BODY_FROM_PARENT_START,

    /**
     * Arguments or parameters align with the first argument/parameter. If the first
     * argument/parameter is on a new line, it's indented from the line containing the opening
     * parenthesis.
     */
    ARGUMENT_LIST,

    /**
     * Continuation lines align with the first child token of the expression. Primarily used for AND
     * expressions in IF conditions.
     */
    ALIGN_TO_FIRST_CHILD,

    /**
     * Right-hand side of assignments is indented one level from the start of the line containing
     * the assignment operator. Used for both regular assignments (<<) and variable definitions
     * (_local, _constant, etc.).
     */
    ASSIGNMENT_RIGHT_HAND_SIDE,

    /**
     * Collection elements ({...}) are indented from the opening brace. The closing brace aligns
     * with the start of the line containing the opening brace.
     */
    COLLECTION_BODY,

    /**
     * Method/procedure invocation chains are indented from the start of the containing statement.
     * Supports fluent interface patterns like: obj.method1(). method2(). method3()
     */
    INVOCATION_CHAIN,

    /**
     * Clause keywords (_loop, _protection, _finally, _when) align with their parent clause or
     * construct. Body content within the clause is indented one level. End keywords align with the
     * clause keyword.
     */
    CLAUSE_FROM_PARENT,

    /**
     * Binary expressions respect operator precedence for alignment. Higher-precedence sub
     * expressions align with their start token; lower-precedence align with their parent.
     */
    EXPRESSION_PRECEDENCE
  }

  /** AST node types that serve as indentation anchors. Derived from STRATEGY_BY_NODE_TYPE keys. */
  private static final AstNodeType[] PARENT_NODE_TYPES;

  /**
   * Maps AST node types to their indentation strategies.
   *
   * <p>This map defines the indentation behavior for each supported node type. Node types not in
   * this map will delegate to their parent node's strategy.
   */
  private static final Map<AstNodeType, IndentStrategy> STRATEGY_BY_NODE_TYPE =
      Map.ofEntries(
          Map.entry(MagikGrammar.MAGIK, IndentStrategy.TOP_LEVEL),
          Map.entry(MagikGrammar.IF, IndentStrategy.BODY_FROM_PARENT_START),
          Map.entry(MagikGrammar.METHOD_DEFINITION, IndentStrategy.BODY_FROM_PARENT_START),
          Map.entry(MagikGrammar.PROCEDURE_DEFINITION, IndentStrategy.BODY_FROM_PARENT_START),
          Map.entry(MagikGrammar.BLOCK, IndentStrategy.BODY_FROM_PARENT_START),
          Map.entry(MagikGrammar.PROTECT, IndentStrategy.CLAUSE_FROM_PARENT),
          Map.entry(MagikGrammar.PROTECTION, IndentStrategy.CLAUSE_FROM_PARENT),
          Map.entry(MagikGrammar.FINALLY, IndentStrategy.CLAUSE_FROM_PARENT),
          Map.entry(MagikGrammar.LOOP, IndentStrategy.CLAUSE_FROM_PARENT),
          Map.entry(MagikGrammar.OVER, IndentStrategy.CLAUSE_FROM_PARENT),
          Map.entry(MagikGrammar.FOR, IndentStrategy.CLAUSE_FROM_PARENT),
          Map.entry(MagikGrammar.TRY, IndentStrategy.BODY_FROM_PARENT_START),
          Map.entry(MagikGrammar.WHEN, IndentStrategy.CLAUSE_FROM_PARENT),
          Map.entry(MagikGrammar.CATCH, IndentStrategy.BODY_FROM_PARENT_START),
          Map.entry(MagikGrammar.LOCK, IndentStrategy.BODY_FROM_PARENT_START),
          Map.entry(MagikGrammar.ARGUMENTS, IndentStrategy.ARGUMENT_LIST),
          Map.entry(MagikGrammar.PARAMETERS, IndentStrategy.ARGUMENT_LIST),
          Map.entry(MagikGrammar.AND_EXPRESSION, IndentStrategy.ALIGN_TO_FIRST_CHILD),
          Map.entry(MagikGrammar.OR_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
          Map.entry(MagikGrammar.XOR_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
          Map.entry(MagikGrammar.EQUALITY_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
          Map.entry(MagikGrammar.RELATIONAL_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
          Map.entry(MagikGrammar.ADDITIVE_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
          Map.entry(MagikGrammar.MULTIPLICATIVE_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
          Map.entry(MagikGrammar.EXPONENTIAL_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
          Map.entry(MagikGrammar.UNARY_EXPRESSION, IndentStrategy.EXPRESSION_PRECEDENCE),
          Map.entry(MagikGrammar.ASSIGNMENT_EXPRESSION, IndentStrategy.ASSIGNMENT_RIGHT_HAND_SIDE),
          Map.entry(
              MagikGrammar.VARIABLE_DEFINITION_STATEMENT,
              IndentStrategy.ASSIGNMENT_RIGHT_HAND_SIDE),
          Map.entry(MagikGrammar.SIMPLE_VECTOR, IndentStrategy.COLLECTION_BODY),
          Map.entry(MagikGrammar.TUPLE, IndentStrategy.COLLECTION_BODY),
          Map.entry(MagikGrammar.METHOD_INVOCATION, IndentStrategy.INVOCATION_CHAIN),
          Map.entry(MagikGrammar.PROCEDURE_INVOCATION, IndentStrategy.INVOCATION_CHAIN));

  static {
    PARENT_NODE_TYPES = STRATEGY_BY_NODE_TYPE.keySet().toArray(AstNodeType[]::new);
  }

  /**
   * Maps keywords to the node type they should unwind to. Used by getParentNode to handle end
   * keywords and clause keywords that need to find their matching construct.
   */
  private static final Map<MagikKeyword, AstNodeType> KEYWORD_TO_NODE_TYPE =
      Map.ofEntries(
          Map.entry(MagikKeyword.THEN, MagikGrammar.IF),
          Map.entry(MagikKeyword.ELSE, MagikGrammar.IF),
          Map.entry(MagikKeyword.ELIF, MagikGrammar.IF),
          Map.entry(MagikKeyword.ENDIF, MagikGrammar.IF),
          Map.entry(MagikKeyword.ENDPROC, MagikGrammar.PROCEDURE_DEFINITION),
          Map.entry(MagikKeyword.ENDMETHOD, MagikGrammar.METHOD_DEFINITION),
          Map.entry(MagikKeyword.ENDBLOCK, MagikGrammar.BLOCK),
          Map.entry(MagikKeyword.FOR, MagikGrammar.FOR),
          Map.entry(MagikKeyword.OVER, MagikGrammar.OVER),
          Map.entry(MagikKeyword.LOOP, MagikGrammar.LOOP),
          Map.entry(MagikKeyword.ENDLOOP, MagikGrammar.LOOP),
          Map.entry(MagikKeyword.PROTECT, MagikGrammar.PROTECT),
          Map.entry(MagikKeyword.ENDPROTECT, MagikGrammar.PROTECT),
          Map.entry(MagikKeyword.PROTECTION, MagikGrammar.PROTECTION),
          Map.entry(MagikKeyword.FINALLY, MagikGrammar.FINALLY),
          Map.entry(MagikKeyword.WHEN, MagikGrammar.WHEN));

  /**
   * Operator precedence order for binary expressions.
   *
   * <p>Lower index = lower precedence. Used by {@link IndentStrategy#EXPRESSION_PRECEDENCE} to
   * determine alignment when multiple binary operators appear in an expression.
   */
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

  /** The current AST node being processed during tree traversal. */
  private AstNode currentNode;

  /** The last token processed (including whitespace). */
  private Token lastToken;

  /** The last non-whitespace token processed. Used for line-change detection. */
  private Token lastTextToken;

  /**
   * Constructor.
   *
   * @param options Formatting options including tab size and whether to use spaces.
   * @param tokenEditor Token trivia editor for modifying whitespace tokens.
   */
  RelativeIndentWalker(final FormattingOptions options, final TokenTriviaEditor tokenEditor) {
    super(options, tokenEditor);
  }

  /** {@inheritDoc} Tracks the current node as we descend into the AST. */
  @Override
  protected void walkPreDefault(final AstNode node) {
    this.currentNode = node;
  }

  /** {@inheritDoc} Restores the parent node as we ascend out of the AST. */
  @Override
  protected void walkPostDefault(final AstNode node) {
    this.currentNode = this.currentNode.getParent();
  }

  /**
   * {@inheritDoc} Handles indentation of comment tokens (trivia).
   *
   * <p>Comments on their own line are indented based on the current context. Comments at the end of
   * a line with code are left as-is.
   */
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

  /**
   * {@inheritDoc} Handles indentation of code tokens.
   *
   * <p>Only tokens on new lines (not on the same line as the previous token) are processed for
   * indentation. Tokens on the same line as their predecessor are left as-is.
   */
  @Override
  protected void walkToken(final Token token) {
    if (!token.isOnSameLineThan(this.lastTextToken)) {
      this.handleTokenNode(token, this.currentNode);
    }

    this.lastToken = token;
    this.lastTextToken = token;
  }

  /**
   * Determines the parent node and dispatches to the appropriate indentation handler.
   *
   * @param token The token to indent.
   * @param node The AST node containing the token.
   */
  private void handleTokenNode(final Token token, final AstNode node) {
    final AstNode parentNode = this.getParentNode(token, node);

    this.indentTokenNode(token, parentNode);
  }

  /**
   * Indents a comment token that appears on its own line.
   *
   * @param token The previous non-whitespace token.
   * @param commentToken The comment token to indent.
   */
  private void walkLineCommentToken(final Token token, final Token commentToken) {
    final AstNode usedNode =
        commentToken.getLine() < this.currentNode.getToken().getLine()
            ? this.currentNode.getParent()
            : this.currentNode;
    this.indentTokenNode(commentToken, usedNode);
  }

  /**
   * Core indentation dispatcher. Looks up the strategy for the parent node type and delegates to
   * the appropriate indentation method.
   *
   * <p>If no strategy is found for the node type, recursively checks the parent node until a
   * strategy is found or falls back to zero indentation.
   *
   * @param token The token to indent.
   * @param parentNode The AST node that determines the indentation strategy.
   */
  private void indentTokenNode(final Token token, final AstNode parentNode) {
    final IndentStrategy strategy = STRATEGY_BY_NODE_TYPE.get(parentNode.getType());
    if (strategy == null) {
      final AstNode parent = parentNode.getParent();
      if (parent != null) {
        this.indentTokenNode(token, parent);
        return;
      }

      this.ensureIndent(token, 0);
      return;
    }

    switch (strategy) {
      case TOP_LEVEL -> this.ensureIndent(token, 0);
      case BODY_FROM_PARENT_START -> this.indentFromParentStart(token, parentNode);
      case ARGUMENT_LIST -> this.indentArgumentOrParameter(token, parentNode);
      case ALIGN_TO_FIRST_CHILD -> this.indentAlignedToFirstChild(token, parentNode);
      case ASSIGNMENT_RIGHT_HAND_SIDE -> this.indentAssignmentRhs(token, parentNode);
      case COLLECTION_BODY -> this.indentCollection(token, parentNode);
      case INVOCATION_CHAIN -> this.indentInvocation(token, parentNode);
      case CLAUSE_FROM_PARENT -> this.indentClauseFromParent(token, parentNode);
      case EXPRESSION_PRECEDENCE -> this.indentExpressionWithPrecedence(token, parentNode);
      default -> throw new IllegalStateException("Unhandled indent strategy: " + strategy);
    }
  }

  /**
   * Indents body content one level from the parent's start token.
   *
   * <p>Structural keywords align with the start keyword rather than being indented. This includes:
   *
   * <ul>
   *   <li>IF: _then, _else, _elif, _endif
   *   <li>METHOD/PROC/BLOCK: _endmethod, _endproc, _endblock
   * </ul>
   *
   * @param token The token to indent.
   * @param parentNode The parent AST node (method, procedure, block, or if).
   */
  private void indentFromParentStart(final Token token, final AstNode parentNode) {
    final Token referenceToken = parentNode.getToken();

    // Structural keywords line up with the start keyword, not indented from it.
    if (this.tokenIs(
        token,
        MagikKeyword.THEN,
        MagikKeyword.ELSE,
        MagikKeyword.ELIF,
        MagikKeyword.ENDIF,
        MagikKeyword.ENDPROC,
        MagikKeyword.ENDMETHOD,
        MagikKeyword.ENDBLOCK)) {
      if (token != referenceToken) {
        this.ensureIndentLinedUpWith(token, referenceToken);
      }
      return;
    }

    this.ensureIndentFrom(token, referenceToken);
  }

  /**
   * Indents arguments or parameters within a method/procedure call or definition.
   *
   * <p>If the first argument is on a new line, it's indented from the line containing the opening
   * parenthesis. Subsequent arguments align with the first argument.
   *
   * @param token The token to indent.
   * @param parentNode The ARGUMENTS or PARAMETERS AST node.
   */
  private void indentArgumentOrParameter(final Token token, final AstNode parentNode) {
    final AstNode firstNode =
        parentNode.getFirstChild(MagikGrammar.ARGUMENT, MagikGrammar.PARAMETER);
    final Token firstNodeToken = firstNode.getToken();
    if (token == firstNodeToken) {
      final Token parentToken = parentNode.getToken();
      final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(parentToken);
      this.ensureIndentFrom(token, firstTextTokenOnLine);
    } else {
      this.ensureIndentLinedUpWith(token, firstNodeToken);
    }
  }

  /**
   * Aligns continuation lines with the first child of an expression.
   *
   * <p>Primarily used for AND expressions within IF conditions, where continuation lines should
   * align with the first operand.
   *
   * @param token The token to indent.
   * @param parentNode The expression AST node.
   */
  private void indentAlignedToFirstChild(final Token token, final AstNode parentNode) {
    final AstNode firstChild = parentNode.getFirstChild();
    if (firstChild == null) {
      return;
    }

    final Token referenceToken = firstChild.getToken();
    if (token == referenceToken) {
      return;
    }

    // Don't reindent tokens that are already indented inside AND expressions in IF conditions
    if (this.isInAndExpressionInsideIf(parentNode) && token.getColumn() > 0) {
      return;
    }

    this.ensureIndentLinedUpWith(token, referenceToken);
  }

  /**
   * Indents the right-hand side of an assignment expression.
   *
   * <p>The RHS is indented one level from the start of the line containing the assignment. This
   * applies to both regular assignments (<<) and variable definitions (_local, _constant, etc.).
   *
   * @param token The token to indent.
   * @param parentNode The ASSIGNMENT_EXPRESSION or VARIABLE_DEFINITION_STATEMENT AST node.
   */
  private void indentAssignmentRhs(final Token token, final AstNode parentNode) {
    final AstNode firstChild = parentNode.getFirstChild();
    final Token referenceToken = firstChild.getToken();
    if (token == referenceToken) {
      throw new IllegalStateException("Should not happen");
    }

    final Token parentToken = parentNode.getToken();
    final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(parentToken);
    this.ensureIndentFrom(token, firstTextTokenOnLine);
  }

  /**
   * Indents elements within a collection (simple vector or tuple).
   *
   * <p>Collection elements are indented one level from the opening brace. The closing brace aligns
   * with the start of the line containing the opening brace, not with the brace itself.
   *
   * @param token The token to indent.
   * @param parentNode The SIMPLE_VECTOR or TUPLE AST node.
   */
  private void indentCollection(final Token token, final AstNode parentNode) {
    final AstNode firstChild = parentNode.getFirstChild();
    final Token referenceToken = firstChild.getToken();
    final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(referenceToken);
    // Choose indent anchor: within argument/parameter lists, indent from the brace (align like
    // first argument). Otherwise, indent from the start of the line (assignment/standalone).
    final boolean inArguments =
      parentNode.getFirstAncestor(MagikGrammar.ARGUMENTS, MagikGrammar.PARAMETERS) != null;
    final Token indentFromToken = inArguments ? referenceToken : firstTextTokenOnLine;

    // Closing braces should line up with the start of the line, not the opening brace
    if (this.tokenIs(token, MagikPunctuator.BRACE_R)) {
      this.ensureIndentLinedUpWith(token, firstTextTokenOnLine);
      return;
    }

    this.ensureIndentFrom(token, indentFromToken);
  }

  /**
   * Indents method/procedure invocation chains.
   *
   * <p>Invocation chains (fluent interfaces) are indented from the start of the containing
   * statement or expression. This supports patterns like: {@code obj.method1().method2().method3()}
   *
   * @param token The token to indent.
   * @param invocationNode The METHOD_INVOCATION or PROCEDURE_INVOCATION AST node.
   */
  private void indentInvocation(final Token token, final AstNode invocationNode) {
    final AstNode indentFromNode =
        invocationNode.getFirstAncestor(MagikGrammar.STATEMENT, MagikGrammar.EXPRESSION);
    final Token indentFromToken = indentFromNode.getToken();
    this.ensureIndentFrom(token, indentFromToken);
  }

  /**
   * Indents clause constructs (loop, protect, try/when, etc.).
   *
   * <p>Clause keywords (_loop, _protection, _finally, _when) align with their parent clause or the
   * root construct. End keywords (_endloop, _endprotect, _endtry, _endcatch, _endlock) align with
   * the root clause keyword. Body content is indented one level from the clause keyword.
   *
   * @param token The token to indent.
   * @param clauseNode The clause AST node (LOOP, PROTECTION, FINALLY, WHEN, etc.).
   */
  private void indentClauseFromParent(final Token token, final AstNode clauseNode) {
    final Token clauseToken = clauseNode.getToken();
    final Token rootClauseToken = this.findRootClauseToken(clauseNode);

    if (this.tokenIs(
        token,
        MagikKeyword.ENDLOOP,
        MagikKeyword.ENDPROTECT,
        MagikKeyword.ENDTRY,
        MagikKeyword.ENDCATCH,
        MagikKeyword.ENDLOCK)) {
      // End keywords line up with the root clause keyword (e.g., _endloop with _for in _for _over
      // _loop).
      if (token != rootClauseToken) {
        this.ensureIndentLinedUpWith(token, rootClauseToken);
      }
      return;
    }

    if (token == clauseToken) {
      // Clause keyword itself - line up with root clause token.
      if (token != rootClauseToken) {
        this.ensureIndentLinedUpWith(token, rootClauseToken);
      }
      return;
    }

    this.ensureIndentFrom(token, clauseToken);
  }

  /**
   * Finds the root clause token by walking up through nested clause constructs.
   *
   * <p>For example, in {@code a << _for _over _loop}, this finds {@code _for} as the root. This
   * ensures that nested loop constructs align properly.
   *
   * @param clauseNode The starting clause node.
   * @return The root clause token for alignment.
   */
  private Token findRootClauseToken(final AstNode clauseNode) {
    AstNode current = clauseNode;
    Token rootToken = current.getToken();

    while (current.getParent() != null) {
      final AstNode parent = current.getParent();
      final IndentStrategy parentStrategy = STRATEGY_BY_NODE_TYPE.get(parent.getType());
      if (parentStrategy == IndentStrategy.CLAUSE_FROM_PARENT) {
        // Parent is also a clause node, so keep walking up
        current = parent;
        rootToken = current.getToken();
      } else if (parentStrategy == IndentStrategy.BODY_FROM_PARENT_START) {
        // Parent is a body-providing node (e.g., TRY for WHEN), use its token as root
        rootToken = parent.getToken();
        break;
      } else {
        // Parent is not a clause node, stop here
        break;
      }
    }

    // Return the root clause token directly.
    // For `a << _for _over _loop`, this returns `_for`, not `a`.
    return rootToken;
  }

  /**
   * Indents binary expressions respecting operator precedence.
   *
   * <p>Higher-precedence sub-expressions align with their start token. Lower-precedence expressions
   * align with their parent. Special handling exists for AND expressions within IF conditions to
   * preserve user formatting.
   *
   * @param token The token to indent.
   * @param expressionNode The binary expression AST node.
   */
  private void indentExpressionWithPrecedence(final Token token, final AstNode expressionNode) {
    final Token nodeToken = expressionNode.getToken();
    final AstNode parentNode = expressionNode.getParent();

    // Don't reindent tokens that are already indented inside AND expressions in IF conditions
    if (token.getColumn() > 0 && this.isInAndExpressionInsideIf(expressionNode)) {
      return;
    }

    // First token of expression - indent from enclosing construct
    if (token == nodeToken) {
      final AstNode enclosingConstruct = this.findEnclosingConstruct(expressionNode);
      if (enclosingConstruct != null && !enclosingConstruct.is(MagikGrammar.MAGIK)) {
        final Token constructToken = enclosingConstruct.getToken();
        final Token firstTextTokenOnLine = this.getFirstTextTokenOnLine(constructToken);
        this.ensureIndentFrom(token, firstTextTokenOnLine);
      }
      return;
    }

    // Handle operator precedence for nested expressions
    if (parentNode != null
        && RelativeIndentWalker.OPERATOR_PRECEDENCE_NODE_TYPES.contains(parentNode.getType())) {
      final Token parentToken = parentNode.getToken();
      final int precedence = OPERATOR_PRECEDENCE_NODE_TYPES.indexOf(expressionNode.getType());
      final int parentPrecedence = OPERATOR_PRECEDENCE_NODE_TYPES.indexOf(parentNode.getType());
      if (precedence > parentPrecedence && nodeToken.getLine() == parentToken.getLine()) {
        this.ensureIndentLinedUpWith(token, nodeToken);
      } else {
        this.ensureIndentLinedUpWith(token, parentToken);
      }
      return;
    }

    this.ensureIndentLinedUpWith(token, nodeToken);
  }

  /** Checks if a node is inside an AND_EXPRESSION that's inside an IF condition. */
  private boolean isInAndExpressionInsideIf(final AstNode node) {
    final AstNode andExpr =
        node.is(MagikGrammar.AND_EXPRESSION)
            ? node
            : node.getFirstAncestor(MagikGrammar.AND_EXPRESSION);
    if (andExpr == null) {
      return false;
    }
    return andExpr.getFirstAncestor(MagikGrammar.IF) != null;
  }

  /** Finds the enclosing construct (method, block, if, loop, etc.) for indentation. */
  private AstNode findEnclosingConstruct(final AstNode node) {
    return node.getFirstAncestor(
        MagikGrammar.METHOD_DEFINITION,
        MagikGrammar.PROCEDURE_DEFINITION,
        MagikGrammar.BLOCK,
        MagikGrammar.IF,
        MagikGrammar.PROTECT,
        MagikGrammar.PROTECTION,
        MagikGrammar.LOOP,
        MagikGrammar.OVER,
        MagikGrammar.FOR,
        MagikGrammar.MAGIK);
  }

  /**
   * Determines the appropriate parent node for indentation purposes.
   *
   * <p>This method handles special cases where the default ancestor lookup doesn't produce the
   * correct indentation anchor. For example, end keywords need to find their matching start
   * keyword, and certain tokens need to skip past intermediate nodes.
   *
   * @param token The token being indented.
   * @param node The AST node containing the token.
   * @return The AST node to use as the indentation anchor.
   */
  private AstNode getParentNode(final Token token, final AstNode node) {
    if (node == null) {
      throw new IllegalStateException("No parent node available for token: " + token);
    }

    if (node.is(MagikGrammar.MAGIK)) {
      return node;
    }

    // For collection closing braces, use the collection node directly.
    if (this.tokenIs(token, MagikPunctuator.BRACE_R)
        && node.is(MagikGrammar.SIMPLE_VECTOR, MagikGrammar.TUPLE)) {
      return node;
    }

    // Check if this keyword maps to a specific node type
    final AstNodeType keywordNodeType = this.getKeywordNodeType(token);
    if (keywordNodeType != null && node.is(keywordNodeType)) {
      return node;
    }

    // Handle comma inside arguments/parameters
    if (this.tokenIs(token, MagikPunctuator.COMMA)
        && node.is(MagikGrammar.ARGUMENTS, MagikGrammar.PARAMETERS)) {
      return node;
    }

    AstNode parentNode = node.getFirstAncestor(RelativeIndentWalker.PARENT_NODE_TYPES);

    // Handle first token of assignment - use the parent of parentNode
    if (parentNode != null
        && parentNode.is(MagikGrammar.ASSIGNMENT_EXPRESSION)
        && parentNode.getToken() == token) {
      return this.getParentNode(token, parentNode);
    }

    // Handle first token of variable definition
    if (node.is(MagikGrammar.VARIABLE_DEFINITION_MODIFIER)) {
      return this.getParentNode(token, parentNode);
    }

    // Find node with a defined strategy
    while (parentNode != null && !STRATEGY_BY_NODE_TYPE.containsKey(parentNode.getType())) {
      parentNode = parentNode.getParent();
    }

    return parentNode == null ? node : parentNode;
  }

  /**
   * Gets the node type that a keyword token should unwind to.
   *
   * @param token The token to check.
   * @return The node type, or null if token is not a mapped keyword.
   */
  private AstNodeType getKeywordNodeType(final Token token) {
    for (final Map.Entry<MagikKeyword, AstNodeType> entry : KEYWORD_TO_NODE_TYPE.entrySet()) {
      if (this.tokenIs(token, entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * Tests if a token matches any of the given Magik keywords.
   *
   * @param token The token to test.
   * @param keywords The keywords to match against.
   * @return True if the token matches any keyword.
   */
  private boolean tokenIs(final Token token, final MagikKeyword... keywords) {
    final String[] keywordValues =
        Stream.of(keywords).map(MagikKeyword::getValue).toArray(String[]::new);
    return AstQuery.tokenIs(token, keywordValues);
  }

  /**
   * Tests if a token matches any of the given Magik punctuators.
   *
   * @param token The token to test.
   * @param punctuators The punctuators to match against.
   * @return True if the token matches any punctuator.
   */
  private boolean tokenIs(final Token token, final MagikPunctuator... punctuators) {
    final String[] punctuatorValues =
        Stream.of(punctuators).map(MagikPunctuator::getValue).toArray(String[]::new);
    return AstQuery.tokenIs(token, punctuatorValues);
  }

  /**
   * Finds the first non-whitespace token on the same line as the given token.
   *
   * <p>This is used to determine the start of a line for alignment purposes, ignoring leading
   * whitespace.
   *
   * @param token The token to start from.
   * @return The first non-whitespace token on the same line.
   */
  private Token getFirstTextTokenOnLine(final Token token) {
    Token currentToken = token;
    Token lastTextToken = token;
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
   * Calculates the visual column position of a token, accounting for tabs.
   *
   * <p>Tabs are expanded to their configured width. This ensures correct alignment regardless of
   * whether the source uses tabs or spaces.
   *
   * @param referenceToken The token to calculate the indent size from.
   * @return The visual column position.
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

  /**
   * Ensures the token has the specified indentation.
   *
   * <p>If the token already has the correct indentation, no changes are made. Otherwise, the
   * leading whitespace is added, removed, or modified as needed.
   *
   * @param token The token to indent.
   * @param indentSize The desired indent size in columns.
   */
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
