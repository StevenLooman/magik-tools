package org.stevenlooman.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ScopeEntry {

  /**
   * Type of ScopeEntry.
   * Types:
   * LOCAL: _local
   * GLOBAL: _global
   * DYNAMIC: _dynamic
   * IMPORT: _import
   * RECURSIVE: _recursive
   * CONSTANT: _constant
   * PARAMETER: procedure parameter
   * DEFINITION: direct assignment
   */
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
  List<AstNode> usages;


  /**
   * Constructor.
   * @param type Type of entry
   * @param identifier Identifier of entry
   * @param node Node of entry
   * @param parentEntry Parent of entry, in case of an _import
   */
  public ScopeEntry(Type type, String identifier, AstNode node, @Nullable ScopeEntry parentEntry) {
    this.type = type;
    this.identifier = identifier;
    this.node = node;
    this.parentEntry = parentEntry;
    this.usages = new ArrayList<>();

    // Parent entry/import is usage
    if (parentEntry != null) {
      parentEntry.addUsage(node);
    }
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

  @CheckForNull
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

  public void addUsage(AstNode node) {
    usages.add(node);
  }

  public List<AstNode> getUsages() {
    return usages;
  }

}
