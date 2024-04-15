package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ForNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;

/** Expression handler. */
class ExpressionHandler extends LocalTypeReasonerHandler {

  private static final Map<String, String> UNARY_OPERATOR_METHODS =
      Map.of(
          MagikOperator.NOT.getValue(), "not",
          MagikKeyword.NOT.getValue(), "not",
          MagikOperator.MINUS.getValue(), "negated",
          MagikOperator.PLUS.getValue(), "unary_plus",
          MagikKeyword.SCATTER.getValue(), "for_scatter()");
  private static final CommentInstructionReader.Instruction TYPE_INSTRUCTION =
      new CommentInstructionReader.Instruction(
          "type", CommentInstructionReader.Instruction.Sort.STATEMENT);
  private static final CommentInstructionReader.Instruction ITER_TYPE_INSTRUCTION =
      new CommentInstructionReader.Instruction(
          "iter-type", CommentInstructionReader.Instruction.Sort.STATEMENT);

  private final CommentInstructionReader instructionReader;

  /**
   * Constructor.
   *
   * @param state Reasoner state.
   */
  ExpressionHandler(final LocalTypeReasonerState state) {
    super(state);
    this.instructionReader =
        new CommentInstructionReader(
            this.state.getMagikFile(), Set.of(TYPE_INSTRUCTION, ITER_TYPE_INSTRUCTION));
  }

  /**
   * Handle binary expression.
   *
   * @param node BINARY_EXPRESSION node.
   */
  void handleBinaryExpression(final AstNode node) {
    // Take left hand side as current.
    final AstNode currentNode = node.getFirstChild();
    ExpressionResultString result = this.state.getNodeType(currentNode);

    final List<AstNode> chainNodes = new ArrayList<>(node.getChildren());
    chainNodes.remove(0);
    for (int i = 0; i < chainNodes.size() - 1; i += 2) {
      // Get operator.
      final AstNode operatorNode = chainNodes.get(i);
      final String operatorStr = operatorNode.getTokenValue().toLowerCase();

      // Get right hand side.
      final AstNode rightNode = chainNodes.get(i + 1);
      final ExpressionResultString rightResult = this.state.getNodeType(rightNode);

      // Evaluate binary operator.
      final TypeString leftTypeStr = result.get(0, TypeString.SW_UNSET);
      final TypeString rightTypeStr = rightResult.get(0, TypeString.SW_UNSET);
      switch (operatorStr.toLowerCase()) {
        case "_is", "_isnt":
          result = new ExpressionResultString(TypeString.SW_FALSE);
          break;

        case "_andif", "_orif":
          // Returns RHS if LHS is true.
          final TypeString combinedType = TypeString.combine(TypeString.SW_FALSE, rightTypeStr);
          result = new ExpressionResultString(combinedType);
          break;

        default:
          final BinaryOperatorDefinition binOpDef =
              this.definitionKeeper
                  .getBinaryOperatorDefinitions(operatorStr, leftTypeStr, rightTypeStr)
                  .stream()
                  .findAny()
                  .orElse(null);
          final TypeString resultingTypeRef =
              binOpDef != null ? binOpDef.getResultTypeName() : TypeString.UNDEFINED;
          result = new ExpressionResultString(resultingTypeRef);
          break;
      }
    }

    // Apply operator to operands and store result.
    this.state.setNodeType(node, result);
  }

  /**
   * Handle augmented assignment expression.
   *
   * @param node AUGMENTED_ASSIGNMENT_EXPRESSION node.
   */
  void handleAugmentedAssignmentExpression(final AstNode node) {
    // Take result from right hand.
    final AstNode rightNode = node.getLastChild();
    final ExpressionResultString rightResult = this.state.getNodeType(rightNode);

    // Get left hand result.
    final AstNode assignedNode = node.getFirstChild();
    final ExpressionResultString leftResult = this.state.getNodeType(assignedNode);

    // Get operator.
    final AstNode operatorNode = node.getChildren().get(1);
    final String operatorStr = operatorNode.getTokenValue();

    // Evaluate binary operator.
    final TypeString leftTypeStr = leftResult.get(0, TypeString.SW_UNSET);
    final TypeString rightTypeStr = rightResult.get(0, TypeString.SW_UNSET);
    final ExpressionResultString result;
    switch (operatorStr.toLowerCase()) {
      case "_is", "_isnt":
        result = new ExpressionResultString(TypeString.SW_FALSE);
        break;

      case "_andif", "_orif":
        // Returns RHS if LHS is true.
        final TypeString combinedTypeStr = TypeString.combine(TypeString.SW_FALSE, rightTypeStr);
        result = new ExpressionResultString(combinedTypeStr);
        break;

      default:
        final BinaryOperatorDefinition binOpDef =
            this.definitionKeeper
                .getBinaryOperatorDefinitions(operatorStr, leftTypeStr, rightTypeStr)
                .stream()
                .findAny()
                .orElse(null);
        final TypeString resultingTypeRef =
            binOpDef != null ? binOpDef.getResultTypeName() : TypeString.UNDEFINED;
        result = new ExpressionResultString(resultingTypeRef);
        break;
    }

    // Store result of expression.
    this.state.setNodeType(node, result);

    if (assignedNode.is(MagikGrammar.ATOM)) {
      this.state.setNodeType(assignedNode, result);

      // Store 'active' type for future reference.
      final GlobalScope globalScope = this.getGlobalScope();
      final Scope scope = globalScope.getScopeForNode(assignedNode);
      Objects.requireNonNull(scope);

      final AstNode identifierNode = assignedNode.getFirstChild(MagikGrammar.IDENTIFIER);
      if (identifierNode != null) {
        final ScopeEntry scopeEntry = scope.getScopeEntry(identifierNode);
        Objects.requireNonNull(scopeEntry);
        this.state.setCurrentScopeEntryNode(scopeEntry, assignedNode);
      }
    }
  }

  /**
   * Handle unary expression.
   *
   * @param node UNARY_EXPRESSION node.
   */
  void handleUnaryExpression(final AstNode node) {
    if (node.getTokenValue().equalsIgnoreCase(MagikKeyword.ALLRESULTS.getValue())) {
      this.assignAtom(node, TypeString.SW_SIMPLE_VECTOR); // TODO: Generics?
      return;
    }

    // Get operand.
    final AstNode operatedNode = node.getLastChild();
    final ExpressionResultString operatedResult = this.state.getNodeType(operatedNode);
    final TypeString typeStr = operatedResult.get(0, TypeString.SW_UNSET);

    // Get operator.
    final String operatorStr = node.getTokenValue().toLowerCase();
    final String operatorMethod = UNARY_OPERATOR_METHODS.get(operatorStr);

    // Apply opertor to operand and store result.
    final ExpressionResultString result =
        this.getMethodInvocationResult(typeStr, operatorMethod)
            .substituteType(TypeString.SELF, typeStr);

    this.state.setNodeType(node, result);
  }

  /**
   * Handle tuple.
   *
   * @param node TUPLE node.
   */
  void handleTuple(final AstNode node) {
    final List<AstNode> childNodes = node.getChildren(MagikGrammar.EXPRESSION);
    final ExpressionResultString result;
    if (childNodes.size() == 1) {
      final AstNode firstChildNode = childNodes.get(0);
      result = this.state.getNodeType(firstChildNode);
    } else {
      result =
          node.getChildren(MagikGrammar.EXPRESSION).stream()
              .map(this.state::getNodeType)
              .map(expressionResult -> expressionResult.get(0, TypeString.SW_UNSET))
              .collect(ExpressionResultString.COLLECTOR);
    }
    this.state.setNodeType(node, result);
  }

  /**
   * Handle iterable expression.
   *
   * @param node ITERABLE_EXPRESSION node.
   */
  void handleIterableExpression(final AstNode node) {
    final AstNode expressionNode = node.getFirstChild();
    final ExpressionResultString iteratorResult = this.state.getNodeIterType(expressionNode);

    // Bind to identifiers, if any.
    final AstNode overNode = node.getParent();
    final AstNode forNode = overNode.getParent();
    if (forNode.is(MagikGrammar.FOR)) {
      final AstNode loopNode = overNode.getFirstChild(MagikGrammar.LOOP);
      final AstNode bodyNode = loopNode.getFirstChild(MagikGrammar.BODY);
      if (bodyNode == null) {
        // Don't error on syntax error.
        return;
      }

      final ForNodeHelper helper = new ForNodeHelper(forNode);
      final List<AstNode> identifierNodes = helper.getLoopIdentifierNodes();
      for (int i = 0; i < identifierNodes.size(); ++i) {
        final AstNode identifierNode = identifierNodes.get(i);
        final AstNode identifierPreviousNode = identifierNode.getPreviousSibling();
        final TypeString typeStr;
        if (identifierPreviousNode != null
            && identifierPreviousNode.getTokenValue().equalsIgnoreCase("_gather")) {
          typeStr = TypeString.SW_SIMPLE_VECTOR;
        } else if (iteratorResult != null) {
          typeStr = iteratorResult.get(i, TypeString.SW_UNSET);
        } else {
          typeStr = TypeString.UNDEFINED;
        }

        final ExpressionResultString result = new ExpressionResultString(typeStr);
        this.state.setNodeType(identifierNode, result);

        final GlobalScope globalScope = this.getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(bodyNode);
        Objects.requireNonNull(scope);
        final ScopeEntry scopeEntry = scope.getScopeEntry(identifierNode);
        Objects.requireNonNull(scopeEntry);
        this.state.setCurrentScopeEntryNode(scopeEntry, identifierNode);
      }
    }
  }

  /**
   * Handle loopbody.
   *
   * @param node LOOPBODY node.
   */
  void handleLoopbody(final AstNode node) {
    // Get results.
    final AstNode multiValueExprNode = node.getFirstChild(MagikGrammar.TUPLE);
    final ExpressionResultString result = this.state.getNodeType(multiValueExprNode);

    // Find related node to store at.
    final AstNode procMethodDefNode =
        node.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION, MagikGrammar.METHOD_DEFINITION);

    // Save results.
    if (this.state.hasNodeType(procMethodDefNode)) {
      // Combine types.
      final ExpressionResultString existingResult = this.state.getNodeType(procMethodDefNode);
      final ExpressionResultString combinedResult =
          new ExpressionResultString(existingResult, result);
      this.state.setNodeIterType(procMethodDefNode, combinedResult);
    } else {
      this.state.setNodeIterType(procMethodDefNode, result);
    }
  }

  /**
   * Handle body.
   *
   * @param node BODY node.
   */
  void handleBody(final AstNode node) {
    // Get result from upper EXPRESSION.
    final AstNode expressionNode = node.getFirstAncestor(MagikGrammar.EXPRESSION);
    if (expressionNode == null) {
      // Happens with a return, don't do anything.
      return;
    }

    ExpressionResultString result = this.state.getNodeType(expressionNode);

    // BODYs don't always have to result in something.
    // Find STATEMENT -> RETURN/EMIT/LEAVE
    AstNode resultingNode =
        AstQuery.getFirstChildFromChain(
            node, MagikGrammar.STATEMENT, MagikGrammar.RETURN_STATEMENT);
    if (resultingNode == null) {
      resultingNode =
          AstQuery.getFirstChildFromChain(
              node, MagikGrammar.STATEMENT, MagikGrammar.EMIT_STATEMENT);
    }
    if (resultingNode == null) {
      resultingNode =
          AstQuery.getFirstChildFromChain(
              node, MagikGrammar.STATEMENT, MagikGrammar.LEAVE_STATEMENT);
    }
    if (resultingNode == null) {
      // Result can also be an unset, as no resulting statement was found.
      // TODO: but... "_block _block _return 1 _endblock _endblock"
      result = new ExpressionResultString(result, ExpressionResultString.EMPTY);
    }

    // Set parent EXPRESSION result.
    this.state.setNodeType(expressionNode, result);
  }

  /**
   * Handle expression.
   *
   * @param node EXPRESSION node.
   */
  void handleExpression(final AstNode node) {
    final AstNode childNode = node.getFirstChild();

    // Copy type of child node to EXPRESSION node.
    final ExpressionResultString callResult = this.state.getNodeType(childNode);
    if (this.state.hasNodeType(childNode) && callResult != ExpressionResultString.UNDEFINED) {
      this.state.setNodeType(node, callResult);
    }

    // Copy iter-type of child node to EXPRESSION node.
    final ExpressionResultString iterCallResult = this.state.getNodeIterType(childNode);
    if (this.state.hasNodeIterType(childNode)
        && iterCallResult != ExpressionResultString.UNDEFINED) {
      this.state.setNodeIterType(node, iterCallResult);
    }

    // Check for type annotations, those overrule normal operations.
    final String typeAnnotation =
        this.instructionReader.getInstructionForNode(node, TYPE_INSTRUCTION);
    if (typeAnnotation != null) {
      final String currentPackage = this.getCurrentPackage(node);
      final ExpressionResultString overrideResultStr =
          TypeStringParser.parseExpressionResultString(typeAnnotation, currentPackage);
      this.state.setNodeType(node, overrideResultStr);
    }

    // Check for iter type annotations, those overrule normal operations.
    final String iterTypeAnnotation =
        this.instructionReader.getInstructionForNode(node, ITER_TYPE_INSTRUCTION);
    if (iterTypeAnnotation != null) {
      final String currentPackage = this.getCurrentPackage(node);
      final ExpressionResultString overrideIterResultStr =
          TypeStringParser.parseExpressionResultString(iterTypeAnnotation, currentPackage);
      this.state.setNodeIterType(node, overrideIterResultStr);
    }
  }

  /**
   * Handle postfix expression.
   *
   * @param node POSTFIX_EXPRESSION node.
   */
  void handlePostfixExpression(final AstNode node) {
    final AstNode rightNode = node.getLastChild();

    // Copy type of child node to POSTFIX_EXPRESSION node.
    final ExpressionResultString callResult = this.state.getNodeType(rightNode);
    this.state.setNodeType(node, callResult);

    // Copy iter-type of child node to POSTFIX_EXPRESSION node.
    final ExpressionResultString iterCallResult = this.state.getNodeIterType(rightNode);
    this.state.setNodeIterType(node, iterCallResult);
  }

  /**
   * Handle method definition.
   *
   * @param node METHOD_DEFINITION node.
   */
  void handleMethodDefinition(final AstNode node) {
    // Technically, a method definition is not an expression... but this has to live somewhere.
    if (!this.state.hasNodeType(node)) {
      // Nothing was assigned to this node, so it must be empty.
      this.state.setNodeType(node, ExpressionResultString.EMPTY);
    }
  }
}
