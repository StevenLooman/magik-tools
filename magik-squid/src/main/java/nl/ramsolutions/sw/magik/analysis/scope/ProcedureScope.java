package nl.ramsolutions.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;
import javax.annotation.Nullable;

/**
 * Procedure-/method-definition scope.
 */
public class ProcedureScope extends Scope {

    ProcedureScope(final Scope parentScope, final AstNode node) {
        super(parentScope, node);
    }

    @Override
    public ScopeEntry getScopeEntry(final String identifier) {
        // Only use current and global scope to get identifier.
        // All variables should be imported or defined.
        if (this.scopeEntries.containsKey(identifier)) {
            return this.scopeEntries.get(identifier);
        }

        // try GlobalScope
        final Scope globalScope = this.getGlobalScope();
        return globalScope.getScopeEntry(identifier);
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

    @Override
    public Scope getProcedureScope() {
        return this;
    }

}
