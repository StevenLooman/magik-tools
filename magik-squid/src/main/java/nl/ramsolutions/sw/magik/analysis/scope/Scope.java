package nl.ramsolutions.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Base scope.
 *
 * <p>Line numbers are 1-based, column numbers are 0-based.
 */
public abstract class Scope {

  /** Scope entries. */
  @SuppressWarnings("checkstyle:VisibilityModifier")
  protected final Map<String, ScopeEntry> scopeEntries = new HashMap<>();

  private final List<Scope> childScopes = new ArrayList<>();
  private final Scope parentScope;
  private final AstNode node;

  /**
   * Constructor.
   *
   * @param parentScope Parent scope.
   * @param node Node.
   */
  protected Scope(final Scope parentScope, final AstNode node) {
    if (!node.is(MagikGrammar.BODY)) {
      throw new IllegalArgumentException();
    }

    this.parentScope = parentScope;
    this.node = node;

    parentScope.addChildScope(this);
  }

  protected Scope(final AstNode node) {
    this.parentScope = null;
    this.node = node;
  }

  protected Scope() {
    this.parentScope = null;
    this.node = null;
  }

  /**
   * Add a child {@link Scope} to self.
   *
   * @param childScope Child scope to add.
   */
  void addChildScope(final Scope childScope) {
    this.childScopes.add(childScope);
  }

  /**
   * Get child {@link Scope}s.
   *
   * @return Children.
   */
  public List<Scope> getChildScopes() {
    return Collections.unmodifiableList(this.childScopes);
  }

  /**
   * Get {@link AstNode} where scope begins.
   *
   * @return {@link AstNode} where scope begins
   */
  public AstNode getNode() {
    return this.node;
  }

  /**
   * Get self and all descendant scopes.
   *
   * @return List with self and descendant scopes.
   */
  public List<Scope> getSelfAndDescendantScopes() {
    final List<Scope> scopes = new ArrayList<>();
    scopes.add(this);
    for (final Scope childScope : this.childScopes) {
      scopes.addAll(childScope.getSelfAndDescendantScopes());
    }
    return scopes;
  }

  /**
   * Get all parent/ancestor scopes.
   *
   * @return List with all parent/ancestor scopes.
   */
  public List<Scope> getAncestorScopes() {
    final List<Scope> scopes = new ArrayList<>();
    Scope ancestorScope = this.getParentScope();
    while (ancestorScope != null) {
      scopes.add(ancestorScope);
      ancestorScope = ancestorScope.getParentScope();
    }
    return scopes;
  }

  /**
   * Get self and all parent/acestor scopes.
   *
   * @return List with self and parent/ancestor scopes.
   */
  public List<Scope> getSelfAndAncestorScopes() {
    final List<Scope> scopes = new ArrayList<>();
    scopes.add(this);

    final List<Scope> ancestorScopes = this.getAncestorScopes();
    scopes.addAll(ancestorScopes);

    return scopes;
  }

  /**
   * Get the parent scope.
   *
   * @return Parent scope.
   */
  public Scope getParentScope() {
    return this.parentScope;
  }

  /**
   * Get the global scope.
   *
   * @return Global scope.
   */
  public Scope getGlobalScope() {
    if (this.parentScope == null) {
      return null;
    }

    return this.parentScope.getGlobalScope();
  }

  /**
   * Get the procedure/method scope.
   *
   * @return Procedure scope.
   */
  public Scope getProcedureScope() {
    if (this.parentScope == null) {
      return null;
    }

    return this.parentScope.getProcedureScope();
  }

  /**
   * Add a ScopeEntry to this scope.
   *
   * @param type Type of declaration.
   * @param identifier Identifier of declaration.
   * @param declarationNode AstNode for declaration.
   * @param parentEntry Parent entry for declaration (used for import declarations.)
   * @return Added ScopeEntry
   */
  public ScopeEntry addDeclaration(
      final ScopeEntry.Type type,
      final String identifier,
      final AstNode declarationNode,
      final @Nullable ScopeEntry parentEntry) {
    if (type == ScopeEntry.Type.DEFINITION && !this.hasScopeEntry(identifier)) {
      final Scope procedureScope = this.getProcedureScope();
      final Scope globalScope = this.getGlobalScope();
      if (procedureScope != null) {
        return procedureScope.addDeclaration(type, identifier, declarationNode, parentEntry);
      } else if (globalScope != null) {
        return globalScope.addDeclaration(type, identifier, declarationNode, parentEntry);
      }
    }

    final ScopeEntry scopeEntry = new ScopeEntry(type, identifier, declarationNode, null);
    scopeEntries.put(identifier, scopeEntry);
    return scopeEntry;
  }

  /**
   * Get a ScopeEntry by its identifier.
   *
   * @param identifier Identifier of the ScopeEntry.
   * @return Scope entry by identifier.
   */
  @CheckForNull
  public ScopeEntry getLocalScopeEntry(final String identifier) {
    return this.scopeEntries.get(identifier);
  }

  /**
   * Get a ScopeEntry by its identifier.
   *
   * @param identifier Identifier of the ScopeEntry.
   * @return Scope entry by identifier.
   */
  @CheckForNull
  public ScopeEntry getScopeEntry(final String identifier) {
    if (this.scopeEntries.containsKey(identifier)) {
      return this.scopeEntries.get(identifier);
    }

    if (parentScope == null) {
      return null;
    }

    return this.parentScope.getScopeEntry(identifier);
  }

  /**
   * Get a ScopeEntry by its identifier {@link AstNode}.
   *
   * @param identifierNode Identifier {@link AstNode} of the ScopeEntry.
   * @return Scope entry by identifier.
   */
  @CheckForNull
  public ScopeEntry getScopeEntry(final AstNode identifierNode) {
    if (identifierNode.isNot(MagikGrammar.IDENTIFIER)) {
      throw new IllegalArgumentException();
    }

    final String identifier = identifierNode.getTokenValue();
    return this.getScopeEntry(identifier);
  }

  /**
   * Check if self has a ScopeEntry by an identifier.
   *
   * @param identifier Identifier of the ScopeEntry.
   * @return true if a ScopeEntry was found, else false.
   */
  public boolean hasScopeEntry(final String identifier) {
    return this.getScopeEntry(identifier) != null;
  }

  /**
   * Get all ScopeEntries in this Scope.
   *
   * @return Collection with all ScopeEntries
   */
  public Collection<ScopeEntry> getScopeEntriesInScope() {
    return this.scopeEntries.values();
  }

  private Token getStartToken() {
    return this.node.getPreviousAstNode().getLastToken();
  }

  private Token getEndToken() {
    return this.node.getNextAstNode().getToken();
  }

  /**
   * Get the start line of this scope, 1-based.
   *
   * @return Start line.
   */
  public int getStartLine() {
    return this.getStartToken().getLine();
  }

  /**
   * Get the start column of this scope, 0-based.
   *
   * @return Start column.
   */
  public int getStartColumn() {
    final Token token = this.getStartToken();
    return token.getColumn() + token.getOriginalValue().length();
  }

  /**
   * Get the end line of this scope, 1-based.
   *
   * @return End line.
   */
  public int getEndLine() {
    return this.getEndToken().getLine();
  }

  /**
   * Get the end column of this scope, 0-based.
   *
   * @return End column.
   */
  public int getEndColumn() {
    return this.getEndToken().getColumn();
  }

  /**
   * Get the most specific {@link Scope} at {@code line}/{@code column}.
   *
   * @param line Line to target.
   * @param column Column to target.
   * @return Scope, if any, at {@code line}/{@code column}.
   */
  @Nullable
  public Scope getScopeForLineColumn(final int line, final int column) {
    if (line < this.getStartLine()
        || line > this.getEndLine()
        || line == this.getStartLine() && column < this.getStartColumn()
        || line == this.getEndLine() && column > this.getEndColumn()) {
      // outside of our scope
      return null;
    }

    // try child Scopes
    for (final Scope childScope : this.childScopes) {
      final Scope foundScope = childScope.getScopeForLineColumn(line, column);
      if (foundScope != null) {
        return foundScope;
      }
    }

    // no child Scope matches, must be ourselves
    return this;
  }
}
