package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.ForNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.BinaryOperator;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
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
    final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);
    final AbstractType falseType = this.typeKeeper.getType(TypeString.SW_FALSE);

    // Take left hand side as current.
    final AstNode currentNode = node.getFirstChild();
    ExpressionResult result = this.state.getNodeType(currentNode);

    final List<AstNode> chainNodes = new ArrayList<>(node.getChildren());
    chainNodes.remove(0);
    for (int i = 0; i < chainNodes.size() - 1; i += 2) {
      // Get operator.
      final AstNode operatorNode = chainNodes.get(i);
      final String operatorStr = operatorNode.getTokenValue().toLowerCase();

      // Get right hand side.
      final AstNode rightNode = chainNodes.get(i + 1);
      final ExpressionResult rightResult = this.state.getNodeType(rightNode);

      // Evaluate binary operator.
      final AbstractType leftType = result.get(0, unsetType);
      final TypeString leftRef = leftType.getTypeString();
      final AbstractType rightType = rightResult.get(0, unsetType);
      final TypeString rightRef = rightType.getTypeString();
      switch (operatorStr.toLowerCase()) {
        case "_is":
        case "_isnt":
          result = new ExpressionResult(falseType);
          break;

        case "_andif":
        case "_orif":
          // Returns RHS if LHS is true.
          final AbstractType combinedType = CombinedType.combine(falseType, rightType);
          result = new ExpressionResult(combinedType);
          break;

        default:
          final BinaryOperator.Operator operator = BinaryOperator.Operator.valueFor(operatorStr);
          final BinaryOperator binaryOperator =
              this.typeKeeper.getBinaryOperator(operator, leftRef, rightRef);
          final TypeString resultingTypeRef =
              binaryOperator != null ? binaryOperator.getResultType() : TypeString.UNDEFINED;
          final AbstractType resultingType = this.typeReader.parseTypeString(resultingTypeRef);
          result = new ExpressionResult(resultingType);
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
    final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);
    final AbstractType falseType = this.typeKeeper.getType(TypeString.SW_FALSE);

    // Take result from right hand.
    final AstNode rightNode = node.getLastChild();
    final ExpressionResult rightResult = this.state.getNodeType(rightNode);

    // Get left hand result.
    final AstNode assignedNode = node.getFirstChild();
    final ExpressionResult leftResult = this.state.getNodeType(assignedNode);

    // Get operator.
    final AstNode operatorNode = node.getChildren().get(1);
    final String operatorStr = operatorNode.getTokenValue();

    // Evaluate binary operator.
    final AbstractType leftType = leftResult.get(0, unsetType);
    final TypeString leftRef = leftType.getTypeString();
    final AbstractType rightType = rightResult.get(0, unsetType);
    final TypeString rightRef = rightType.getTypeString();
    final ExpressionResult result;
    switch (operatorStr.toLowerCase()) {
      case "_is":
      case "_isnt":
        result = new ExpressionResult(falseType);
        break;

      case "_andif":
      case "_orif":
        // Returns RHS if LHS is true.
        final AbstractType combinedType = CombinedType.combine(falseType, rightType);
        result = new ExpressionResult(combinedType);
        break;

      default:
        final BinaryOperator.Operator operator = BinaryOperator.Operator.valueFor(operatorStr);
        final BinaryOperator binaryOperator =
            this.typeKeeper.getBinaryOperator(operator, leftRef, rightRef);
        final TypeString resultingTypeRef =
            binaryOperator != null ? binaryOperator.getResultType() : TypeString.UNDEFINED;
        final AbstractType resultingType = this.typeReader.parseTypeString(resultingTypeRef);
        result = new ExpressionResult(resultingType);
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

    final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);

    // Get operand.
    final AstNode operatedNode = node.getLastChild();
    final ExpressionResult operatedResult = this.state.getNodeType(operatedNode);
    final AbstractType type = operatedResult.get(0, unsetType);

    // Get operator.
    final String operatorStr = node.getTokenValue().toLowerCase();
    final String operatorMethod = UNARY_OPERATOR_METHODS.get(operatorStr);

    // Apply opertor to operand and store result.
    final ExpressionResult result =
        this.getMethodInvocationResult(type, operatorMethod)
            .substituteType(SelfType.INSTANCE, type);

    this.state.setNodeType(node, result);
  }

  /**
   * Handle tuple.
   *
   * @param node TUPLE node.
   */
  void handleTuple(final AstNode node) {
    final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);

    final List<AstNode> childNodes = node.getChildren(MagikGrammar.EXPRESSION);
    final ExpressionResult result;
    if (childNodes.size() == 1) {
      final AstNode firstChildNode = childNodes.get(0);
      result = this.state.getNodeType(firstChildNode);
    } else {
      result =
          node.getChildren(MagikGrammar.EXPRESSION).stream()
              .map(this.state::getNodeType)
              .map(expressionResult -> expressionResult.get(0, unsetType))
              .collect(ExpressionResult.COLLECTOR);
    }
    this.state.setNodeType(node, result);
  }

  /**
   * Handle iterable expression.
   *
   * @param node ITERABLE_EXPRESSION node.
   */
  void handleIterableExpression(final AstNode node) {
    final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);
    final AstNode expressionNode = node.getFirstChild();
    final ExpressionResult iteratorResult = this.state.getNodeIterType(expressionNode);

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
        final AbstractType type;
        if (identifierPreviousNode != null
            && identifierPreviousNode.getTokenValue().equalsIgnoreCase("_gather")) {
          type = this.typeKeeper.getType(TypeString.SW_SIMPLE_VECTOR);
        } else if (iteratorResult != null) {
          type = iteratorResult.get(i, unsetType);
        } else {
          type = UndefinedType.INSTANCE;
        }

        final ExpressionResult result = new ExpressionResult(type);
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
    final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);

    // Get results.
    final AstNode multiValueExprNode = node.getFirstChild(MagikGrammar.TUPLE);
    final ExpressionResult result = this.state.getNodeType(multiValueExprNode);

    // Find related node to store at.
    final AstNode procMethodDefNode =
        node.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION, MagikGrammar.METHOD_DEFINITION);

    // Save results.
    if (this.state.hasNodeType(procMethodDefNode)) {
      // Combine types.
      final ExpressionResult existingResult = this.state.getNodeType(procMethodDefNode);
      final ExpressionResult combinedResult =
          new ExpressionResult(existingResult, result, unsetType);
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

    ExpressionResult result = this.state.getNodeType(expressionNode);

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
      final ExpressionResult emptyResult = new ExpressionResult();
      final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);
      result = new ExpressionResult(result, emptyResult, unsetType);
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
    final ExpressionResult callResult = this.state.getNodeType(childNode);
    if (this.state.hasNodeType(childNode) && callResult != ExpressionResult.UNDEFINED) {
      this.state.setNodeType(node, callResult);
    }

    // Copy iter-type of child node to EXPRESSION node.
    final ExpressionResult iterCallResult = this.state.getNodeIterType(childNode);
    if (this.state.hasNodeIterType(childNode) && iterCallResult != ExpressionResult.UNDEFINED) {
      this.state.setNodeIterType(node, iterCallResult);
    }

    // Check for type annotations, those overrule normal operations.
    final String typeAnnotation =
        this.instructionReader.getInstructionForNode(node, TYPE_INSTRUCTION);
    if (typeAnnotation != null) {
      final String currentPackage = this.getCurrentPackage(node);
      final ExpressionResultString overrideResultStr =
          TypeStringParser.parseExpressionResultString(typeAnnotation, currentPackage);
      final ExpressionResult overrideResult =
          this.typeReader.parseExpressionResultString(overrideResultStr);
      this.state.setNodeType(node, overrideResult);
    }

    // Check for iter type annotations, those overrule normal operations.
    final String iterTypeAnnotation =
        this.instructionReader.getInstructionForNode(node, ITER_TYPE_INSTRUCTION);
    if (iterTypeAnnotation != null) {
      final String currentPackage = this.getCurrentPackage(node);
      final ExpressionResultString overrideIterResultStr =
          TypeStringParser.parseExpressionResultString(iterTypeAnnotation, currentPackage);
      final ExpressionResult overrideIterResult =
          this.typeReader.parseExpressionResultString(overrideIterResultStr);
      this.state.setNodeIterType(node, overrideIterResult);
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
    final ExpressionResult callResult = this.state.getNodeType(rightNode);
    this.state.setNodeType(node, callResult);

    // Copy iter-type of child node to POSTFIX_EXPRESSION node.
    final ExpressionResult iterCallResult = this.state.getNodeIterType(rightNode);
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
      final ExpressionResult emptyResult = new ExpressionResult();
      this.state.setNodeType(node, emptyResult);
    }
  }
}
