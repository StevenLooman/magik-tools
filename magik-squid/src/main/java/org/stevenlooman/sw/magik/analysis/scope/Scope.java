package org.stevenlooman.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class Scope {

  protected Map<String, ScopeEntry> scopeEntries = new HashMap<>();
  private List<Scope> childScopes = new ArrayList<>();
  private Scope parentScope;
  private AstNode node;

  Scope(Scope parentScope, AstNode node) {
    this.parentScope = parentScope;
    this.node = node;

    parentScope.addChildScope(this);
  }

  Scope() {
    this.parentScope = null;
    this.node = null;
  }

  void addChildScope(Scope childScope) {
    childScopes.add(childScope);
  }

  public List<Scope> getSelfAndDescendantScopes() {
    List<Scope> scopes = new ArrayList<>();
    scopes.add(this);
    for (Scope childScope: childScopes) {
      scopes.addAll(childScope.getSelfAndDescendantScopes());
    }
    return scopes;
  }

  public Scope getParentScope() {
    return parentScope;
  }

  public Scope getGlobalScope() {
    if (parentScope == null) {
      return null;
    }
    return parentScope.getGlobalScope();
  }

  public Scope getProcedureScope() {
    if (parentScope == null) {
      return null;
    }
    return parentScope.getProcedureScope();
  }


  public ScopeEntry addDeclaration(ScopeEntry.Type type, String identifier, AstNode node, ScopeEntry parentEntry) {
    if (type == ScopeEntry.Type.DEFINITION) {
      if (!hasScopeEntry(identifier)) {
        Scope procedureScope = getProcedureScope();
        Scope globalScope = getGlobalScope();
        if (procedureScope != null) {
          return procedureScope.addDeclaration(type, identifier, node, parentEntry);
        } else if (globalScope != null) {
          return globalScope.addDeclaration(type, identifier, node, parentEntry);
        }
      }
    }

    ScopeEntry scopeEntry = new ScopeEntry(type, identifier, node, null);
    scopeEntries.put(identifier, scopeEntry);
    return scopeEntry;
  }

  public ScopeEntry getScopeEntry(String identifier) {
    if (scopeEntries.containsKey(identifier)) {
      return scopeEntries.get(identifier);
    }

    if (parentScope == null) {
      return null;
    }
    return parentScope.getScopeEntry(identifier);
  }

  public boolean hasScopeEntry(String identifier) {
    return getScopeEntry(identifier) != null;
  }

  public Collection<ScopeEntry> getScopeEntries() {
    return scopeEntries.values();
  }

}
