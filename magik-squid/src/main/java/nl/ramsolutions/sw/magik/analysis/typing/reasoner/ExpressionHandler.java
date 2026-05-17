package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.UnaryOperatorHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.GenericHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;

/** Expression handler. */
class ExpressionHandler extends LocalTypeReasonerHandler {
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
      result = this.getBinaryOperatorDefinition(operatorStr, leftTypeStr, rightTypeStr);
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
    final ExpressionResultString result =
        this.getBinaryOperatorDefinition(operatorStr, leftTypeStr, rightTypeStr);

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

  private ExpressionResultString getBinaryOperatorDefinition(
      final String operatorStr, final TypeString lhsTypeStr, final TypeString rhsTypeStr) {
    final ExpressionResultString result;
    switch (operatorStr.toLowerCase()) {
      case "_is", "_isnt":
        result = new ExpressionResultString(TypeString.SW_FALSE);
        break;

      case "_andif", "_orif":
        // Returns RHS if LHS is true.
        final TypeString combinedTypeStr = TypeString.combine(TypeString.SW_FALSE, rhsTypeStr);
        result = new ExpressionResultString(combinedTypeStr);
        break;

      default:
        // This tries to find the actual applied type via the species method.
        // The species method on an object returns a method_table for a given exemplar.
        // We assume - in our type database - the species method return type has a
        // generic E with the exemplar.
        final TypeString exemplarRef = TypeString.ofGenericReference("E");
        final TypeString lhsMethodTableTypeStr =
            this.typeResolver.getRespondingMethodDefinitions(lhsTypeStr, "species").stream()
                .map(MethodDefinition::getReturnTypes)
                .map(resultStr -> resultStr.get(0, lhsTypeStr))
                .reduce(TypeString::combine)
                .orElse(TypeString.UNDEFINED);
        final TypeString resolvedLhsTypeStr =
            lhsMethodTableTypeStr.getGenericDefinition(exemplarRef) != null
                ? lhsMethodTableTypeStr.getGenericDefinition(exemplarRef).getGenericType()
                : lhsTypeStr;
        final TypeString rhsMethodTableTypeStr =
            this.typeResolver.getRespondingMethodDefinitions(rhsTypeStr, "species").stream()
                .map(MethodDefinition::getReturnTypes)
                .map(resultStr -> resultStr.get(0, rhsTypeStr))
                .reduce(TypeString::combine)
                .orElse(TypeString.UNDEFINED);
        final TypeString resolvedRhsTypeStr =
            rhsMethodTableTypeStr.getGenericDefinition(exemplarRef) != null
                ? rhsMethodTableTypeStr.getGenericDefinition(exemplarRef).getGenericType()
                : rhsTypeStr;

        final BinaryOperatorDefinition binOpDef =
            this.definitionKeeper
                .getBinaryOperatorDefinitions(operatorStr, resolvedLhsTypeStr, resolvedRhsTypeStr)
                .stream()
                .findAny()
                .orElse(null);
        final TypeString resultingTypeRef =
            binOpDef != null ? binOpDef.getResultTypeName() : TypeString.UNDEFINED;
        result = new ExpressionResultString(resultingTypeRef);
        break;
    }

    return result;
  }

  /**
   * Handle unary expression.
   *
   * @param node UNARY_EXPRESSION node.
   */
  void handleUnaryExpression(final AstNode node) {
    final UnaryOperatorHelper helper = new UnaryOperatorHelper(node);
    if (helper.isAllResults()) {
      this.assignAtom(node, TypeString.SW_SIMPLE_VECTOR); // TODO: Generics?
      return;
    }

    // Get operand.
    final AstNode operatedNode = node.getLastChild();
    final ExpressionResultString operatedResult = this.state.getNodeType(operatedNode);
    final TypeString typeStr = operatedResult.get(0, TypeString.SW_UNSET);

    // Get operator.
    final String operatorMethod = helper.getUnaryOperatorMethod();

    // Apply operator to operand and store result.
    final ExpressionResultString invocationResult =
        this.getMethodInvocationResult(typeStr, operatorMethod);
    final ExpressionResultString result =
        new GenericHelper(typeStr)
            .substituteGenerics(invocationResult)
            .substituteType(TypeString.SELF, typeStr)
            .substituteType(TypeString.PRIVATE, typeStr);

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
    final AstNode loopNode = overNode.getFirstChild(MagikGrammar.LOOP);
    final AstNode bodyNode = loopNode.getFirstChild(MagikGrammar.BODY);
    final AstNode forNode = overNode.getParent();
    if (forNode.is(MagikGrammar.FOR)) {
      if (bodyNode == null) {
        // Don't error on syntax error.
        return;
      }

      final AstNode forVariablesNode = forNode.getFirstChild(MagikGrammar.FOR_VARIABLES);
      final AstNode identifiersNode =
          forVariablesNode.getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER);
      this.assignIdentifiersWithGatherTypes(bodyNode, identifiersNode, iteratorResult);
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

    // Store result on this node; the enclosing method/proc handler will collect it.
    this.state.setNodeType(node, result);
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
      // Only apply type override to the outermost EXPRESSION on this line.
      // This prevents nested expressions (like method arguments) from being overridden.
      // The type annotation should only apply to:
      // 1. The top-level expression in RETURN/EMIT statements (handled by StatementHandler).
      // 2. The top-level expression in VARIABLE_DEFINITION (handled by StatementHandler).
      // 3. Other expressions that are the outermost on their line.
      final AstNode parent = node.getParent();
      final boolean isPartOfReturnEmitOrAssign =
          parent != null
              && (parent.is(MagikGrammar.TUPLE)
                      && (parent.getParent().is(MagikGrammar.RETURN_STATEMENT)
                          || parent.getParent().is(MagikGrammar.EMIT_STATEMENT))
                  || parent.is(
                      MagikGrammar.ASSIGNMENT_EXPRESSION,
                      MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION)
                  || parent.is(MagikGrammar.VARIABLE_DEFINITION));

      // Also check if there's an ancestor EXPRESSION on the same line
      final AstNode ancestorExpression = node.getFirstAncestor(MagikGrammar.EXPRESSION);
      final boolean hasAncestorExpressionOnSameLine =
          ancestorExpression != null && ancestorExpression.getTokenLine() == node.getTokenLine();

      if (!isPartOfReturnEmitOrAssign && !hasAncestorExpressionOnSameLine) {
        final String currentPackage = this.getCurrentPackage(node);
        final ExpressionResultString overrideResultStr =
            TypeStringParser.parseExpressionResultString(typeAnnotation, currentPackage);
        this.state.setNodeType(node, overrideResultStr);
      }
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

    // TODO: Is this needed?

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
    // Collect return and iter types from descendant statements.
    this.collectReturnAndIterTypes(node);
  }
}
