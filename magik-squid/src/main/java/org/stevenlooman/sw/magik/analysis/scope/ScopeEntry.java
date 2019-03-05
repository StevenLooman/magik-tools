package org.stevenlooman.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;

import java.util.Objects;

public class ScopeEntry {

  public enum Type {
    LOCAL,
    GLOBAL,
    DYNAMIC,
    IMPORT,
    RECURSIVE,
    CONSTANT,
    PARAMETER,

    DEFINITION,
    ;
  }

  Type type;
  String identifier;
  AstNode node;
  ScopeEntry parentEntry;


  public ScopeEntry(Type type, String identifier, AstNode node, ScopeEntry parentEntry) {
    this.type = type;
    this.identifier = identifier;
    this.node = node;
    this.parentEntry = parentEntry;
  }

  public Type getType() {
    return type;
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public AstNode getNode() {
    return node;
  }

  public ScopeEntry getParentEntry() {
    return parentEntry;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof ScopeEntry)) {
      return false;
    }
    ScopeEntry otherEntry = (ScopeEntry)other;
    return otherEntry.getIdentifier() == getIdentifier();
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier);
  }
}
