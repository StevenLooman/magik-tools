package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.helpers.ContinueLeaveStatementNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Statement handler.
 */
class StatementHandler extends LocalTypeReasonerHandler {

    /**
     * Constructor.
     * @param state Reasoner state.
     */
    StatementHandler(final LocalTypeReasonerState state) {
        super(state);
    }

    /**
     * Handle variable definition.
     * @param node VARIABLE_DEFINITION node.
     */
    void handleVariableDefinition(final AstNode node) {
        // Left side
        final GlobalScope globalScope = this.getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        final String identifier = identifierNode.getTokenValue();
        final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
        Objects.requireNonNull(scopeEntry);

        if (scopeEntry.isType(ScopeEntry.Type.LOCAL)
            || scopeEntry.isType(ScopeEntry.Type.DEFINITION)
            || scopeEntry.isType(ScopeEntry.Type.CONSTANT)) {
            final AstNode expressionNode = node.getFirstChild(MagikGrammar.EXPRESSION);
            final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);
            final ExpressionResult result = expressionNode == null
                ? new ExpressionResult(unsetType)
                : this.state.getNodeType(expressionNode);

            final AstNode scopeEntryNode = scopeEntry.getDefinitionNode();
            this.state.setNodeType(scopeEntryNode, result);

            // TODO: Test if it isn't a slot node.
            this.state.setCurrentScopeEntryNode(scopeEntry, identifierNode);
        } else if (scopeEntry.isType(ScopeEntry.Type.IMPORT)) {
            // TODO: globals/dynamics/...?
            final ScopeEntry importedScopeEntry = scopeEntry.getImportedEntry();
            final AstNode activeImportedNode = this.state.getCurrentScopeEntryNode(importedScopeEntry);
            final ExpressionResult result = this.state.getNodeType(activeImportedNode);
            this.state.setNodeType(node, result);
        }
    }

    /**
     * Handle variable definition multi.
     * @param node VARIABLE_DEFINITION_MULTI node.
     */
    void handleVariableDefinitionMulti(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);

        // Take result for right hand.
        final AstNode rightNode = node.getFirstChild(MagikGrammar.TUPLE);
        final ExpressionResult result = this.state.getNodeType(rightNode);

        // Assign to all left hands.
        final List<AstNode> identifierNodes = node
            .getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER)
            .getChildren(MagikGrammar.IDENTIFIER);
        for (int i = 0; i < identifierNodes.size(); ++i) {
            // TODO: Does this work with gather?
            final AstNode identifierNode = identifierNodes.get(i);
            final ExpressionResult partialResult = new ExpressionResult(result.get(i, unsetType));
            this.state.setNodeType(identifierNode, partialResult);

            // Store 'active' type for future reference.
            final GlobalScope globalScope = this.getGlobalScope();
            final Scope scope = globalScope.getScopeForNode(node);
            Objects.requireNonNull(scope);
            final String identifier = identifierNode.getTokenValue();
            final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
            // TODO: Test if it isn't a slot node.
            this.state.setCurrentScopeEntryNode(scopeEntry, identifierNode);
        }
    }

    /**
     * Handle emit.
     * @param node EMIT node.
     */
    void handleEmit(final AstNode node) {
        // Get results.
        final AstNode tupleNode = node.getFirstChild(MagikGrammar.TUPLE);
        final ExpressionResult result = this.state.getNodeType(tupleNode);

        // Find related node.
        final AstNode bodyNode = node.getFirstAncestor(MagikGrammar.BODY);
        final AstNode expressionNode = bodyNode.getFirstAncestor(
            MagikGrammar.EXPRESSION,  // for BLOCK etc
            MagikGrammar.METHOD_DEFINITION,  // for METHOD_DEFINITION
            MagikGrammar.PROCEDURE_DEFINITION);  // for PROC_DEFINITION

        // Save results.
        this.addNodeType(expressionNode, result);
    }

    /**
     * Handle leave.
     * @param node LEAVE node.
     */
    void handleLeave(final AstNode node) {
        // Get results.
        final AstNode multiValueExprNode = node.getFirstChild(MagikGrammar.TUPLE);
        final ExpressionResult result = multiValueExprNode != null
            ? this.state.getNodeType(multiValueExprNode)
            : new ExpressionResult();

        // Find related BODY/EXPRESION nodes.
        final ContinueLeaveStatementNodeHelper helper = new ContinueLeaveStatementNodeHelper(node);
        final AstNode bodyNode = helper.getRelatedBodyNode();
        final AstNode expressionNode = bodyNode.getFirstAncestor(MagikGrammar.EXPRESSION);
        this.addNodeType(expressionNode, result);
    }

    /**
     * Handle return.
     * @param node RETURN node.
     */
    void handleReturn(final AstNode node) {
        // Get results.
        final AstNode tupleNode = node.getFirstChild(MagikGrammar.TUPLE);
        final ExpressionResult result = tupleNode != null
            ? this.state.getNodeType(tupleNode)
            : new ExpressionResult();

        // Find related node to store on.
        final AstNode definitionNode = node.getFirstAncestor(
            MagikGrammar.METHOD_DEFINITION,
            MagikGrammar.PROCEDURE_DEFINITION);

        // Save results at returned node.
        this.addNodeType(definitionNode, result);
    }

}
