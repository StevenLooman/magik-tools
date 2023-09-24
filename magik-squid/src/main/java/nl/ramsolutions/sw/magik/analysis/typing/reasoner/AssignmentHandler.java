package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Assignment handler.
 */
class AssignmentHandler extends LocalTypeReasonerHandler {

    private static final TypeString SW_UNSET = TypeString.ofIdentifier("unset", "sw");

    /**
     * Constructor.
     * @param magikFile MagikFile
     * @param nodeTypes Node types.
     * @param nodeIterTypes Node iter types.
     * @param currentScopeEntryNodes Current scope entry nodes.
     */
    AssignmentHandler(
            final MagikTypedFile magikFile,
            final Map<AstNode, ExpressionResult> nodeTypes,
            final Map<AstNode, ExpressionResult> nodeIterTypes,
            final Map<ScopeEntry, AstNode> currentScopeEntryNodes) {
        super(magikFile, nodeTypes, nodeIterTypes, currentScopeEntryNodes);
    }

    /**
     * Handle assignment.
     * @param node ASSIGNMENT node.
     */
    void handleAssignment(final AstNode node) {
        // Take result from right hand.
        final AstNode rightNode = node.getLastChild();
        ExpressionResult result = this.getNodeType(rightNode);

        // Walking from back to front, assign result to each.
        // If left hand side is a method call, call the method and update result.
        final List<AstNode> assignedNodes = node.getChildren(MagikGrammar.values());
        assignedNodes.remove(rightNode);
        Collections.reverse(assignedNodes);
        for (final AstNode assignedNode : assignedNodes) {
            if (assignedNode.is(MagikGrammar.POSTFIX_EXPRESSION)) {
                // Find 2nd to last type.
                final int index = assignedNode.getChildren().size() - 2;
                final AstNode semiLastChildNode = assignedNode.getChildren().get(index);
                final ExpressionResult invokedResult = this.getNodeType(semiLastChildNode);

                // Get the result of the invocation.
                final AstNode lastChildNode = assignedNode.getLastChild();
                final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(lastChildNode);
                final String methodName = helper.getMethodName();
                final AbstractType type = invokedResult.get(0, null);
                if (type == null
                    || methodName == null) {
                    return;
                }
                result = this.getMethodInvocationResult(type, methodName);
            } else if (assignedNode.is(MagikGrammar.ATOM)
                       && assignedNode.getFirstChild(MagikGrammar.IDENTIFIER) != null) {
                // Store 'active' type for future reference.
                final GlobalScope globalScope = this.magikFile.getGlobalScope();
                final Scope scope = globalScope.getScopeForNode(assignedNode);
                Objects.requireNonNull(scope);
                final String identifier = assignedNode.getTokenValue();
                final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
                this.currentScopeEntryNodes.put(scopeEntry, assignedNode);

                this.setNodeType(assignedNode, result);
            } else if (assignedNode.is(MagikGrammar.ATOM)
                       && assignedNode.getFirstChild(MagikGrammar.SLOT) != null) {
                // Store slot.
                this.setNodeType(assignedNode, result);
            } else {
                throw new IllegalStateException();
            }
        }

        // Store result of complete expression.
        this.setNodeType(node, result);
    }

    /**
     * Handle multiple assignment.
     * @param node MULTIPLE_ASSIGNMENT node.
     */
    void handleMultipleAssignment(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        // Take result for right hand.
        final AstNode rightNode = node.getLastChild();
        final ExpressionResult result = this.getNodeType(rightNode);

        // Assign to all left hands.
        final AstNode assignablesNode = node.getFirstChild(MagikGrammar.MULTIPLE_ASSIGNMENT_ASSIGNABLES);
        final List<AstNode> expressionNodes =
            assignablesNode.getChildren(MagikGrammar.EXPRESSION);
        for (int i = 0; i < expressionNodes.size(); ++i) {
            final AstNode expressionNode = expressionNodes.get(i);
            final ExpressionResult partialResult = new ExpressionResult(result.get(i, unsetType));
            this.setNodeType(expressionNode, partialResult);

            final AstNode identifierNode = AstQuery.getOnlyFromChain(
                expressionNode,
                MagikGrammar.ATOM,
                MagikGrammar.IDENTIFIER);
            if (identifierNode != null) {
                // Store 'active' type for future reference.
                final GlobalScope globalScope = this.magikFile.getGlobalScope();
                final Scope scope = globalScope.getScopeForNode(node);
                Objects.requireNonNull(scope);
                final String identifier = identifierNode.getTokenValue();
                final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
                // TODO: Test if it isn't a slot node.
                this.currentScopeEntryNodes.put(scopeEntry, expressionNode);
            }
        }
    }

}
