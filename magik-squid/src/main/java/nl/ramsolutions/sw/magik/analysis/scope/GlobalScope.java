package nl.ramsolutions.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Global scope.
 */
public class GlobalScope extends Scope {

    private final Map<AstNode, Scope> scopeIndex;

    GlobalScope(final Map<AstNode, Scope> scopeIndex, final AstNode node) {
        super(node);
        this.scopeIndex = scopeIndex;
    }

    GlobalScope() {
        this.scopeIndex = new HashMap<>();
    }

    @Override
    public Scope getGlobalScope() {
        return this;
    }

    @Override
    public ScopeEntry addDeclaration(
            final ScopeEntry.Type type,
            final String identifier,
            final AstNode node,
            final @Nullable ScopeEntry parentEntry) {
        final ScopeEntry scopeEntry = new ScopeEntry(type, identifier, node, parentEntry);
        this.scopeEntries.put(identifier, scopeEntry);
        return scopeEntry;
    }

    /**
     * Get the Scope for a AstNode.
     * @param node Node to look for
     * @return Scope for node, or global scope if node is not found.
     */
    @CheckForNull
    public Scope getScopeForNode(final AstNode node) {
        // Try node directly, perhaps we're lucky.
        final Scope scope = this.scopeIndex.get(node);
        if (scope != null) {
            return scope;
        }

        AstNode searchNode = node;
        // Do some helping.
        if (node.is(MagikGrammar.PARAMETER)
            || node.is(MagikGrammar.PARAMETERS)) {
            searchNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
            searchNode = searchNode.getFirstDescendant(MagikGrammar.BODY);
        }

        // If this is a for-loop-variable, use the loop-body scope.
        // TODO: In upper part a PARAMETER is ok, but here it has to be a token-node...
        final AstNode forNode = AstQuery.getParentFromChain(
            searchNode,
            MagikGrammar.IDENTIFIERS_WITH_GATHER,
            MagikGrammar.FOR_VARIABLES,
            MagikGrammar.FOR);
        if (forNode != null) {
            searchNode = forNode.getFirstDescendant(MagikGrammar.BODY);
        }

        if (!searchNode.is(MagikGrammar.BODY)) {
            searchNode = searchNode.getFirstAncestor(MagikGrammar.BODY, MagikGrammar.MAGIK);
        }

        return this.scopeIndex.get(searchNode);
    }

    @Override
    public int getStartLine() {
        return 1;
    }

    @Override
    public int getStartColumn() {
        return 0;
    }

    @Override
    public int getEndLine() {
        final AstNode lastChild = this.getNode().getLastChild();
        return lastChild.getTokenLine();
    }

    @Override
    public int getEndColumn() {
        final AstNode lastChild = this.getNode().getLastChild();
        final Token token = lastChild.getToken();
        return token.getColumn() + token.getOriginalValue().length();
    }

}
