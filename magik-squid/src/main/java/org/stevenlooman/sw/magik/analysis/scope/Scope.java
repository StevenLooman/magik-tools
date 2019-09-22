package org.stevenlooman.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public abstract class Scope {

  protected Map<String, ScopeEntry> scopeEntries = new HashMap<>();
  private List<Scope> childScopes = new ArrayList<>();
  private Scope parentScope;
  private AstNode node;

  Scope(Scope parentScope, AstNode node) {
    this.parentScope = parentScope;
    this.node = node;

    parentScope.addChildScope(this);
  }

  protected Scope(AstNode node) {
    this.node = node;
  }

  Scope() {
    this.parentScope = null;
    this.node = null;
  }

  /**
   * Add a child scope to self.
   * @param childScope Child scope to add.
   */
  void addChildScope(Scope childScope) {
    childScopes.add(childScope);
  }

  /**
   * Get {{AstNode}} where scope begins.
   * @return {{AstNode}} where scope begins
   */
  public AstNode getNode() {
    return node;
  }

  /**
   * Get self and all descendant scopes.
   * @return List with self and descendant scopes.
   */
  public List<Scope> getSelfAndDescendantScopes() {
    List<Scope> scopes = new ArrayList<>();
    scopes.add(this);
    for (Scope childScope: childScopes) {
      scopes.addAll(childScope.getSelfAndDescendantScopes());
    }
    return scopes;
  }

  /**
   * Get all parent/ancestor scopes.
   * @return List with all parent/ancestor scopes.
   */
  public List<Scope> getAncestorScopes() {
    List<Scope> scopes = new ArrayList<>();
    Scope parentScope = getParentScope();
    while (parentScope != null) {
      scopes.add(parentScope);
      parentScope = parentScope.getParentScope();
    }
    return scopes;
  }

  /**
   * Get self and all parent/acestor scopes.
   * @return List with self and parent/ancestor scopes.
   */
  public List<Scope> getSelfAndAncestorScopes() {
    List<Scope> scopes = new ArrayList<>();
    scopes.add(this);
    List<Scope> ancestorScopes = getAncestorScopes();
    scopes.addAll(ancestorScopes);
    return scopes;
  }

  /**
   * Get the parent scope.
   * @return Parent scope.
   */
  public Scope getParentScope() {
    return parentScope;
  }

  /**
   * Get the global scope
   * @return Global scope.
   */
  public Scope getGlobalScope() {
    if (parentScope == null) {
      return null;
    }
    return parentScope.getGlobalScope();
  }

  /**
   * Get the procedure/method scope.
   * @return Procedure scope.
   */
  public Scope getProcedureScope() {
    if (parentScope == null) {
      return null;
    }
    return parentScope.getProcedureScope();
  }

  /**
   * Add a ScopeEntry to this scope.
   * @param type Type of declaration.
   * @param identifier Identifier of declaration.
   * @param node AstNode for declaration.
   * @param parentEntry Parent entry for declaration (used for import declarations.)
   * @return Added ScopeEntry
   */
  public ScopeEntry addDeclaration(ScopeEntry.Type type,
                                   String identifier,
                                   AstNode node,
                                   @Nullable ScopeEntry parentEntry) {
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

  /**
   * Get a ScopeEntry by its identifier.
   * @param identifier Identifier of the ScopeEntry.
   * @return Scope entry by identifier.
   */
  @CheckForNull
  public ScopeEntry getScopeEntry(String identifier) {
    if (scopeEntries.containsKey(identifier)) {
      return scopeEntries.get(identifier);
    }

    if (parentScope == null) {
      return null;
    }
    return parentScope.getScopeEntry(identifier);
  }

  /**
   * Check if self has a ScopeEntry by an identifier.
   * @param identifier Identifier of the ScopeEntry.
   * @return true if a ScopeEntry was found, else false.
   */
  public boolean hasScopeEntry(String identifier) {
    return getScopeEntry(identifier) != null;
  }

  /**
   * Get all ScopeEntries in this Scope.
   * @return Collection with all ScopeEntries
   */
  public Collection<ScopeEntry> getScopeEntries() {
    return scopeEntries.values();
  }

  /**
   * Get the start line of this scope.
   * @return Start line.
   */
  public int getStartLine() {
    AstNode parentNode = node.getParent();
    List<Token> tokens = parentNode.getTokens();
    Token firstToken = tokens.get(0);
    return firstToken.getLine();
  }

  /**
   * Get the start column of this scope.
   * @return Start column.
   */
  public int getStartColumn() {
    AstNode parentNode = node.getParent();
    List<Token> tokens = parentNode.getTokens();
    Token firstToken = tokens.get(0);
    return firstToken.getColumn() + firstToken.getOriginalValue().length();
  }

  /**
   * Get the end line of this scope.
   * @return End line.
   */
  public int getEndLine() {
    AstNode parentNode = node.getParent();
    List<Token> tokens = parentNode.getTokens();
    Token lastToken = tokens.get(tokens.size() - 1);
    return lastToken.getLine();
  }

  /**
   * Get the end column of this scope.
   * @return End column.
   */
  public int getEndColumn() {
    AstNode parentNode = node.getParent();
    List<Token> tokens = parentNode.getTokens();
    Token lastToken = tokens.get(tokens.size() - 1);
    return lastToken.getColumn();
  }

  /**
   * Get the most specific {{Scope}} at {{line}}/{{column}}.
   * @param line Line to target.
   * @param column Column to target.
   * @return Scope, if any, at {{line}}/{{column}}.
   */
  @Nullable
  public Scope getScopeForLineColumn(int line, int column) {
    if (line < getStartLine()
        || line > getEndLine()
        || line == getStartLine() && column < getStartColumn()
        || line == getEndLine() && column > getEndColumn()) {
      // outside of our scope
      return null;
    }

    // try child Scopes
    for (Scope childScope: childScopes) {
      Scope foundScope = childScope.getScopeForLineColumn(line, column);
      if (foundScope != null) {
        return foundScope;
      }
    }

    // no child Scope matches, must be ourselves
    return this;
  }

}
