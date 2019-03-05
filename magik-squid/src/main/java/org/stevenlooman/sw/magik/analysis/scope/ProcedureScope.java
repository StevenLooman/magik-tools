package org.stevenlooman.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;

public class ProcedureScope extends Scope {

  ProcedureScope(Scope parentScope, AstNode node) {
    super(parentScope, node);
  }

  @Override
  public ScopeEntry getScopeEntry(String identifier) {
    // Only use current and global scope to get identifier.
    // All variables should be imported or defined.
    if (scopeEntries.containsKey(identifier)) {
      return scopeEntries.get(identifier);
    }

    // try GlobalScope
    Scope globalScope = getGlobalScope();
    return globalScope.getScopeEntry(identifier);
  }

  @Override
  public ScopeEntry addDeclaration(ScopeEntry.Type type, String identifier, AstNode node, ScopeEntry parentEntry) {
    ScopeEntry scopeEntry = new ScopeEntry(type, identifier, node, parentEntry);
    scopeEntries.put(identifier, scopeEntry);
    return scopeEntry;
  }

  @Override
  public Scope getProcedureScope() {
    return this;
  }

}
