package nl.ramsolutions.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;

/** Body scope. */
public class BodyScope extends Scope {

  BodyScope(final Scope parentScope, final AstNode node) {
    super(parentScope, node);
  }

  /**
   * Add try variable alias entry.
   *
   * @param scopeEntry {@link ScopeEntry} to add/alias.
   */
  public void addTryVariableAlias(final ScopeEntry scopeEntry) {
    if (!scopeEntry.isType(ScopeEntry.Type.LOCAL)) {
      throw new IllegalArgumentException();
    }

    final String identifier = scopeEntry.getIdentifier();
    this.scopeEntries.put(identifier, scopeEntry);
  }
}
