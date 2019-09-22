package org.stevenlooman.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;

import java.util.HashMap;
import java.util.Map;

public class GlobalScope extends Scope {

  private Map<AstNode, Scope> scopeIndex;

  GlobalScope(Map<AstNode, Scope> scopeIndex, AstNode node) {
    super(node);
    this.scopeIndex = scopeIndex;
  }

  GlobalScope() {
    this.scopeIndex = new HashMap<AstNode, Scope>();
  }

  @Override
  public Scope getGlobalScope() {
    return this;
  }

  @Override
  public ScopeEntry addDeclaration(ScopeEntry.Type type,
                                   String identifier,
                                   AstNode node,
                                   ScopeEntry parentEntry) {
    ScopeEntry scopeEntry = new ScopeEntry(type, identifier, node, parentEntry);
    scopeEntries.put(identifier, scopeEntry);
    return scopeEntry;
  }

  /**
   * Get the Scope for a AstNode
   * @param node Node to look for
   * @return Scope for node, or global scope if node is not found.
   */
  public Scope getScopeForNode(AstNode node) {
    // find scope for this node
    AstNode currentNode = node;
    while (currentNode != null) {
      if (scopeIndex.containsKey(currentNode)) {
        return scopeIndex.get(currentNode);
      }

      currentNode = currentNode.getParent();
    }

    return this; // get global scope
  }

  @Override
  public int getStartLine() {
    return 0;
  }

  @Override
  public int getStartColumn() {
    return 0;
  }

  @Override
  public int getEndLine() {
    return Integer.MAX_VALUE;
  }

  @Override
  public int getEndColumn() {
    return Integer.MAX_VALUE;
  }

}
