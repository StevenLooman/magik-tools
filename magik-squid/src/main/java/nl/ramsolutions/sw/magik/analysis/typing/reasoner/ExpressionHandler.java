package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.magik.MagikTypedFile;
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

/**
 * Expression handler.
 */
class ExpressionHandler extends LocalTypeReasonerHandler {

    private static final TypeString SW_UNSET = TypeString.ofIdentifier("unset", "sw");
    private static final TypeString SW_FALSE = TypeString.ofIdentifier("false", "sw");
    private static final TypeString SW_SIMPLE_VECTOR = TypeString.ofIdentifier("simple_vector", "sw");
    private static final Map<String, String> UNARY_OPERATOR_METHODS = Map.of(
        MagikOperator.NOT.getValue(), "not",
        MagikKeyword.NOT.getValue(), "not",
        MagikOperator.MINUS.getValue(), "negated",
        MagikOperator.PLUS.getValue(), "unary_plus",
        MagikKeyword.SCATTER.getValue(), "for_scatter()");
    private static final CommentInstructionReader.InstructionType TYPE_INSTRUCTION =
        CommentInstructionReader.InstructionType.createStatementInstructionType("type");
    private static final CommentInstructionReader.InstructionType ITER_TYPE_INSTRUCTION =
        CommentInstructionReader.InstructionType.createStatementInstructionType("iter-type");

    private final CommentInstructionReader instructionReader;

    /**
     * Constructor.
     * @param magikFile MagikFile
     * @param nodeTypes Node types.
     * @param nodeIterTypes Node iter types.
     * @param currentScopeEntryNodes Current scope entry nodes.
     */
    ExpressionHandler(
            final MagikTypedFile magikFile,
            final Map<AstNode, ExpressionResult> nodeTypes,
            final Map<AstNode, ExpressionResult> nodeIterTypes,
            final Map<ScopeEntry, AstNode> currentScopeEntryNodes) {
        super(magikFile, nodeTypes, nodeIterTypes, currentScopeEntryNodes);
        this.instructionReader = new CommentInstructionReader(
            magikFile, Set.of(TYPE_INSTRUCTION, ITER_TYPE_INSTRUCTION));
    }

    /**
     * Handle binary expression.
     * @param node BINARY_EXPRESSION node.
     */
    void handleBinaryExpression(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
        final AbstractType falseType = this.typeKeeper.getType(SW_FALSE);

        // Take left hand side as current.
        final AstNode currentNode = node.getFirstChild();
        ExpressionResult result = this.getNodeType(currentNode);

        final List<AstNode> chainNodes = new ArrayList<>(node.getChildren());
        chainNodes.remove(0);
        for (int i = 0; i < chainNodes.size() - 1; i += 2) {
            // Get operator.
            final AstNode operatorNode = chainNodes.get(i);
            final String operatorStr = operatorNode.getTokenValue().toLowerCase();

            // Get right hand side.
            final AstNode rightNode = chainNodes.get(i + 1);
            final ExpressionResult rightResult = this.getNodeType(rightNode);

            // Evaluate binary operator.
            final AbstractType leftType = result.get(0, unsetType);
            final AbstractType rightType = rightResult.get(0, unsetType);
            switch (operatorStr.toLowerCase()) {
                case "_is":
                case "_isnt":
                    result = new ExpressionResult(falseType);
                    break;

                case "_andif":
                case "_orif":
                    // Returns RHS if LHS is true.
                    final CombinedType combinedType = new CombinedType(falseType, rightType);
                    result = new ExpressionResult(combinedType);
                    break;

                default:
                    final BinaryOperator.Operator operator = BinaryOperator.Operator.valueFor(operatorStr);
                    final BinaryOperator binaryOperator =
                        this.typeKeeper.getBinaryOperator(operator, leftType, rightType);
                    final TypeString resultingTypeRef = binaryOperator != null
                        ? binaryOperator.getResultType()
                        : TypeString.UNDEFINED;
                    final AbstractType resultingType = this.typeReader.parseTypeString(resultingTypeRef);
                    result = new ExpressionResult(resultingType);
                    break;
            }
        }

        // Apply operator to operands and store result.
        this.setNodeType(node, result);
    }

    /**
     * Handle augmented assignment expression.
     * @param node AUGMENTED_ASSIGNMENT_EXPRESSION node.
     */
    void handleAugmentedAssignmentExpression(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
        final AbstractType falseType = this.typeKeeper.getType(SW_FALSE);

        // Take result from right hand.
        final AstNode rightNode = node.getLastChild();
        final ExpressionResult rightResult = this.getNodeType(rightNode);

        // Get left hand result.
        final AstNode assignedNode = node.getFirstChild();
        final ExpressionResult leftResult = this.getNodeType(assignedNode);

        // Get operator.
        final AstNode operatorNode = node.getChildren().get(1);
        final String operatorStr = operatorNode.getTokenValue();

        // Evaluate binary operator.
        final AbstractType leftType = leftResult.get(0, unsetType);
        final AbstractType rightType = rightResult.get(0, unsetType);
        final ExpressionResult result;
        switch (operatorStr.toLowerCase()) {
            case "_is":
            case "_isnt":
                result = new ExpressionResult(falseType);
                break;

            case "_andif":
            case "_orif":
                // Returns RHS if LHS is true.
                final CombinedType combinedType = new CombinedType(falseType, rightType);
                result = new ExpressionResult(combinedType);
                break;

            default:
                final BinaryOperator.Operator operator = BinaryOperator.Operator.valueFor(operatorStr);
                final BinaryOperator binaryOperator = this.typeKeeper.getBinaryOperator(operator, leftType, rightType);
                final TypeString resultingTypeRef = binaryOperator != null
                    ? binaryOperator.getResultType()
                    : TypeString.UNDEFINED;
                final AbstractType resultingType = this.typeReader.parseTypeString(resultingTypeRef);
                result = new ExpressionResult(resultingType);
                break;
        }

        // Store result of expression.
        this.setNodeType(node, result);

        if (assignedNode.is(MagikGrammar.ATOM)) {
            // Store 'active' type for future reference.
            final GlobalScope globalScope = magikFile.getGlobalScope();
            final Scope scope = globalScope.getScopeForNode(assignedNode);
            Objects.requireNonNull(scope);

            final String identifier = assignedNode.getTokenValue();
            final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
            this.currentScopeEntryNodes.put(scopeEntry, assignedNode);

            this.setNodeType(assignedNode, result);
        }
    }

    /**
     * Handle unary expression.
     * @param node UNARY_EXPRESSION node.
     */
    void handleUnaryExpression(final AstNode node) {
        if (node.getTokenValue().equalsIgnoreCase(MagikKeyword.ALLRESULTS.getValue())) {
            this.assignAtom(node, SW_SIMPLE_VECTOR);  // TODO: Generics?
            return;
        }

        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        // Get operand.
        final AstNode operatedNode = node.getLastChild();
        final ExpressionResult operatedResult = this.getNodeType(operatedNode);
        final AbstractType type = operatedResult.get(0, unsetType);

        // Get operator.
        final String operatorStr = node.getTokenValue().toLowerCase();
        final String operatorMethod = UNARY_OPERATOR_METHODS.get(operatorStr);

        // Apply opertor to operand and store result.
        final ExpressionResult result =
            this.getMethodInvocationResult(type, operatorMethod).substituteType(SelfType.INSTANCE, type);

        this.setNodeType(node, result);
    }

    /**
     * Handle tuple.
     * @param node TUPLE node.
     */
    void handleTuple(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        final List<AstNode> childNodes = node.getChildren(MagikGrammar.EXPRESSION);
        final ExpressionResult result;
        if (childNodes.size() == 1) {
            final AstNode firstChildNode = childNodes.get(0);
            result = this.getNodeType(firstChildNode);
        } else {
            result = node.getChildren(MagikGrammar.EXPRESSION).stream()
                .map(this::getNodeType)
                .map(expressionResult -> expressionResult.get(0, unsetType))
                .collect(ExpressionResult.COLLECTOR);
        }
        this.setNodeType(node, result);
    }

    /**
     * Handle iterable expression.
     * @param node ITERABLE_EXPRESSION node.
     */
    void handleIterableExpression(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
        final AstNode expressionNode = node.getFirstChild();
        final ExpressionResult iteratorResult = this.getNodeIterType(expressionNode);

        // Bind to identifiers, if any.
        final AstNode overNode = node.getParent();
        final AstNode forNode = overNode.getParent();
        if (forNode.is(MagikGrammar.FOR)) {
            final AstNode loopNode = overNode.getFirstChild(MagikGrammar.LOOP);
            final AstNode bodyNode = loopNode.getFirstChild(MagikGrammar.BODY);
            final ForNodeHelper helper = new ForNodeHelper(forNode);
            final List<AstNode> identifierNodes = helper.getLoopIdentifierNodes();
            for (int i = 0; i < identifierNodes.size(); ++i) {
                final AstNode identifierNode = identifierNodes.get(i);
                final AstNode identifierPreviousNode = identifierNode.getPreviousSibling();
                final AbstractType type;
                if (identifierPreviousNode != null
                    && identifierPreviousNode.getTokenValue().equalsIgnoreCase("_gather")) {
                    type = this.typeKeeper.getType(SW_SIMPLE_VECTOR);
                } else if (iteratorResult != null) {
                    type = iteratorResult.get(i, unsetType);
                } else {
                    type = UndefinedType.INSTANCE;
                }

                final ExpressionResult result = new ExpressionResult(type);
                this.setNodeType(identifierNode, result);

                final GlobalScope globalScope = this.magikFile.getGlobalScope();
                final Scope scope = globalScope.getScopeForNode(bodyNode);
                Objects.requireNonNull(scope);
                final String identifier = identifierNode.getTokenValue();
                final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
                this.currentScopeEntryNodes.put(scopeEntry, identifierNode);
            }
        }
    }

    /**
     * Handle loopbody.
     * @param node LOOPBODY node.
     */
    void handleLoopbody(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        // Get results.
        final AstNode multiValueExprNode = node.getFirstChild(MagikGrammar.TUPLE);
        final ExpressionResult result = this.getNodeType(multiValueExprNode);

        // Find related node to store at.
        final AstNode procMethodDefNode = node.getFirstAncestor(
            MagikGrammar.PROCEDURE_DEFINITION, MagikGrammar.METHOD_DEFINITION);

        // Save results.
        if (this.hasNodeType(procMethodDefNode)) {
            // Combine types.
            final ExpressionResult existingResult = this.getNodeType(procMethodDefNode);
            final ExpressionResult combinedResult = new ExpressionResult(existingResult, result, unsetType);
            this.setNodeIterType(procMethodDefNode, combinedResult);
        } else {
            this.setNodeIterType(procMethodDefNode, result);
        }
    }

    /**
     * Handle body.
     * @param node BODY node.
     */
    void handleBody(final AstNode node) {
        // Get result from upper EXPRESSION.
        final AstNode expressionNode = node.getFirstAncestor(MagikGrammar.EXPRESSION);
        if (expressionNode == null) {
            // Happens with a return, don't do anything.
            return;
        }

        ExpressionResult result = this.getNodeType(expressionNode);

        // BODYs don't always have to result in something.
        // Find STATEMENT -> RETURN/EMIT/LEAVE
        AstNode resultingNode = AstQuery.getFirstChildFromChain(
            node, MagikGrammar.STATEMENT, MagikGrammar.RETURN_STATEMENT);
        if (resultingNode == null) {
            resultingNode = AstQuery.getFirstChildFromChain(node, MagikGrammar.STATEMENT, MagikGrammar.EMIT_STATEMENT);
        }
        if (resultingNode == null) {
            resultingNode = AstQuery.getFirstChildFromChain(node, MagikGrammar.STATEMENT, MagikGrammar.LEAVE_STATEMENT);
        }
        if (resultingNode == null) {
            // Result can also be an unset, as no resulting statement was found.
            // TODO: but... "_block _block _return 1 _endblock _endblock"
            final ExpressionResult emptyResult = new ExpressionResult();
            final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
            result = new ExpressionResult(result, emptyResult, unsetType);
        }

        // Set parent EXPRESSION result.
        this.setNodeType(expressionNode, result);
    }

    /**
     * Handle expression.
     * @param node EXPRESSION node.
     */
    void handleExpression(final AstNode node) {
        final AstNode childNode = node.getFirstChild();

        // Copy type of child node to EXPRESSION node.
        final ExpressionResult callResult = this.getNodeType(childNode);
        if (this.hasNodeType(childNode)
            && callResult != ExpressionResult.UNDEFINED) {
            this.setNodeType(node, callResult);
        }

        // Copy iter-type of child node to EXPRESSION node.
        final ExpressionResult iterCallResult = this.getNodeIterType(childNode);
        if (this.hasNodeIterType(childNode)
            && iterCallResult != ExpressionResult.UNDEFINED) {
            this.setNodeIterType(node, iterCallResult);
        }

        // Check for type annotations, those overrule normal operations.
        final String typeAnnotation = this.instructionReader.getInstructionForNode(node, TYPE_INSTRUCTION);
        if (typeAnnotation != null) {
            final String currentPackage = this.getCurrentPackage(node);
            final ExpressionResultString overrideResultStr =
                TypeStringParser.parseExpressionResultString(typeAnnotation, currentPackage);
            final ExpressionResult overrideResult =
                this.typeReader.parseExpressionResultString(overrideResultStr);
            this.setNodeType(node, overrideResult);
        }

        // Check for iter type annotations, those overrule normal operations.
        final String iterTypeAnnotation = this.instructionReader.getInstructionForNode(node, ITER_TYPE_INSTRUCTION);
        if (iterTypeAnnotation != null) {
            final String currentPackage = this.getCurrentPackage(node);
            final ExpressionResultString overrideIterResultStr =
                TypeStringParser.parseExpressionResultString(iterTypeAnnotation, currentPackage);
            final ExpressionResult overrideIterResult =
                this.typeReader.parseExpressionResultString(overrideIterResultStr);
            this.setNodeIterType(node, overrideIterResult);
        }
    }

    /**
     * Handle postfix expression.
     * @param node POSTFIX_EXPRESSION node.
     */
    void handlePostfixExpression(final AstNode node) {
        final AstNode rightNode = node.getLastChild();

        // Copy type of child node to POSTFIX_EXPRESSION node.
        final ExpressionResult callResult = this.getNodeType(rightNode);
        this.setNodeType(node, callResult);

        // Copy iter-type of child node to POSTFIX_EXPRESSION node.
        final ExpressionResult iterCallResult = this.getNodeIterType(rightNode);
        this.setNodeIterType(node, iterCallResult);
    }

    /**
     * Handle method definition.
     * @param node METHOD_DEFINITION node.
     */
    void handleMethodDefinition(final AstNode node) {
        // Technically, a method definition is not an expression... but this has to live somewhere.
        if (!this.hasNodeType(node)) {
            // Nothing was assigned to this node, so it must be empty.
            final ExpressionResult emptyResult = new ExpressionResult();
            this.setNodeType(node, emptyResult);
        }
    }

}
