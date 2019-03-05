package org.stevenlooman.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;

public class GlobalScope extends Scope {

  @Override
  public Scope getGlobalScope() {
    return this;
  }

  @Override
  public ScopeEntry addDeclaration(ScopeEntry.Type type, String identifier, AstNode node, ScopeEntry parentEntry) {
    ScopeEntry scopeEntry = new ScopeEntry(type, identifier, node, parentEntry);
    scopeEntries.put(identifier, scopeEntry);
    return scopeEntry;
  }

}
