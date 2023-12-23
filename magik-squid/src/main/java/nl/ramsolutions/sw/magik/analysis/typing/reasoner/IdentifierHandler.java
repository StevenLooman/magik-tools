package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Identifier handler.
 */
class IdentifierHandler extends LocalTypeReasonerHandler {

    private static final TypeString SW_CONDITION = TypeString.ofIdentifier("condition", "sw");

    /**
     * Constructor.
     * @param magikFile MagikFile
     * @param nodeTypes Node types.
     * @param nodeIterTypes Node iter types.
     * @param currentScopeEntryNodes Current scope entry nodes.
     */
    IdentifierHandler(
            final MagikTypedFile magikFile,
            final Map<AstNode, ExpressionResult> nodeTypes,
            final Map<AstNode, ExpressionResult> nodeIterTypes,
            final Map<ScopeEntry, AstNode> currentScopeEntryNodes) {
        super(magikFile, nodeTypes, nodeIterTypes, currentScopeEntryNodes);
    }

    /**
     * Handle identifier.
     * @param node IDENTIFIER node.
     */
    void handleIdentifier(final AstNode node) {
        final AstNode parent = node.getParent();
        if (!parent.is(MagikGrammar.ATOM)) {
            return;
        }

        final GlobalScope globalScope = this.magikFile.getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);
        final String identifier = node.getTokenValue();
        final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
        Objects.requireNonNull(scopeEntry);
        if (scopeEntry.isType(ScopeEntry.Type.GLOBAL)
            || scopeEntry.isType(ScopeEntry.Type.DYNAMIC)) {
            final String currentPackage = this.getCurrentPackage(node);
            final TypeString typeString = TypeString.ofIdentifier(identifier, currentPackage);
            this.assignAtom(node, typeString);
        } else if (scopeEntry.isType(ScopeEntry.Type.IMPORT)) {
            final ScopeEntry parentScopeEntry = scopeEntry.getImportedEntry();
            final AstNode lastNodeType = this.currentScopeEntryNodes.get(parentScopeEntry);
            final ExpressionResult result = this.getNodeType(lastNodeType);
            this.assignAtom(node, result);
        } else if (scopeEntry.isType(ScopeEntry.Type.PARAMETER)) {
            final AstNode parameterNode = scopeEntry.getDefinitionNode();
            final ExpressionResult result = this.getNodeType(parameterNode);
            this.assignAtom(node, result);
        } else {
            final AstNode lastNode = this.currentScopeEntryNodes.get(scopeEntry);
            if (lastNode != null) {
                final ExpressionResult result = this.getNodeType(lastNode);
                this.assignAtom(node, result);
            }
        }
    }

    /**
     * Handle try variable ndoe.
     * @param node TRY_VARIABLE node.
     */
    void handleTryVariable(final AstNode node) {
        final String identifier = node.getTokenValue();

        final AstNode tryNode = node.getParent();
        final List<AstNode> whenNodes = tryNode.getChildren(MagikGrammar.WHEN);
        for (final AstNode whenNode : whenNodes) {
            final AstNode whenBodyNode = whenNode.getFirstChild(MagikGrammar.BODY);
            final GlobalScope globalScope = this.magikFile.getGlobalScope();
            final Scope scope = globalScope.getScopeForNode(whenBodyNode);
            Objects.requireNonNull(scope);
            final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
            this.currentScopeEntryNodes.put(scopeEntry, node);
        }

        final AbstractType conditionType = this.typeKeeper.getType(SW_CONDITION);
        final ExpressionResult result = new ExpressionResult(conditionType);
        this.setNodeType(node, result);
    }

    /**
     * Handle exemplar name.
     * @param node EXEMPLAR_NAME node.
     */
    void handleExemplarName(final AstNode node) {
        final String exemplarName = node.getTokenValue();
        final String currentPackage = this.getCurrentPackage(node);
        final TypeString typeStr = TypeString.ofIdentifier(exemplarName, currentPackage);
        final AbstractType type = this.typeKeeper.getType(typeStr);
        final ExpressionResult result = new ExpressionResult(type);
        this.setNodeType(node, result);
    }

}
