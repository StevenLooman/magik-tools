package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.analysis.scope.GlobalScope;
import org.stevenlooman.sw.magik.analysis.scope.Scope;
import org.stevenlooman.sw.magik.analysis.scope.ScopeEntry;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Rule(key = UnusedVariableCheck.CHECK_KEY)
public class UnusedVariableCheck extends MagikCheck {

  private static final String MESSAGE = "Remove the unused local variable \"%s\".";
  public static final String CHECK_KEY = "UnusedVariable";
  private boolean checkParameters;

  public UnusedVariableCheck() {
    this.checkParameters = false;
  }

  public UnusedVariableCheck(boolean checkParameters) {
    this.checkParameters = checkParameters;
  }

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(MagikGrammar.MAGIK);
  }

  private boolean isAssignedToDirectly(AstNode identifierNode) {
    AstNode variableDefinitionNode = identifierNode.getParent();
    if (variableDefinitionNode.getType() != MagikGrammar.VARIABLE_DEFINITION) {
      return false;
    }

    if (variableDefinitionNode.getFirstChild(MagikGrammar.IDENTIFIER) != identifierNode) {
      return false;
    }

    return variableDefinitionNode.getTokens().stream()
      .anyMatch(token -> token.getValue().equals("<<") || token.getValue().equals("^<<"));
  }

  private boolean isPartOfMultiVariableDefinition(AstNode identifierNode) {
    AstNode identifiersWithGatherNode = identifierNode.getParent();
    if (identifiersWithGatherNode == null
        || identifiersWithGatherNode.getType() != MagikGrammar.IDENTIFIERS_WITH_GATHER) {
      return false;
    }
    AstNode multiVarDeclNode = identifiersWithGatherNode.getParent();
    if (multiVarDeclNode == null
        || multiVarDeclNode.getType() != MagikGrammar.VARIABLE_DEFINITION_MULTI) {
      return false;
    }

    return true;
  }

  private boolean isPartOfMultiAssignment(AstNode identifierNode) {
    AstNode identifiersNode = identifierNode.getParent();
    if (identifiersNode == null
        || identifiersNode.getType() != MagikGrammar.IDENTIFIERS_WITH_GATHER) {
      return false;
    }
    AstNode multiVarDeclNode = identifiersNode.getParent();
    if (multiVarDeclNode == null
        || multiVarDeclNode.getType() != MagikGrammar.MULTIPLE_ASSIGNMENT_STATEMENT) {
      return false;
    }

    return true;
  }

  private boolean anyNextSiblingUsed(AstNode identifierNode) {
    GlobalScope globalScope = getContext().getGlobalScope();
    Scope scope = globalScope.getScopeForNode(identifierNode);
    AstNode sibling = identifierNode.getNextSibling();
    while (sibling != null) {
      if (sibling.getType() != MagikGrammar.IDENTIFIER) {
        sibling = sibling.getNextSibling();
        continue;
      }

      String siblingIdentifier = sibling.getTokenValue();
      ScopeEntry siblingEntry = scope.getScopeEntry(siblingIdentifier);
      if (siblingEntry != null
          && !siblingEntry.getUsages().isEmpty()) {
        return true;
      }

      sibling = sibling.getNextSibling();
    }

    return false;
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.getType() != MagikGrammar.MAGIK) {
      return;
    }

    // Gather all scope entries to be checked
    List<ScopeEntry> scopeEntries = new ArrayList<>();
    GlobalScope globalScope = getContext().getGlobalScope();
    for (Scope scope: globalScope.getSelfAndDescendantScopes()) {
      for (ScopeEntry scopeEntry: scope.getScopeEntries()) {
        AstNode scopeEntryNode = scopeEntry.getNode();

        // But not globals/dynamics which are assigned to directly
        if ((scopeEntry.getType() == ScopeEntry.Type.GLOBAL
             || scopeEntry.getType() == ScopeEntry.Type.DYNAMIC)
            && isAssignedToDirectly(scopeEntryNode)) {
          continue;
        }

        // No parameters, unless forced to
        if (!checkParameters && scopeEntry.getType() == ScopeEntry.Type.PARAMETER) {
          continue;
        }

        scopeEntries.add(scopeEntry);
      }
    }

    // Remove all defined scope entries which are:
    // - part of a MULTI_VARIABLE_DECLARATION
    // - any later identifier(s) of it is used
    // - but this one is not
    for (ScopeEntry entry : new ArrayList<>(scopeEntries)) {
      AstNode entryNode = entry.getNode();
      if ((isPartOfMultiVariableDefinition(entryNode) || isPartOfMultiAssignment(entryNode))
          && anyNextSiblingUsed(entryNode)) {
        scopeEntries.remove(entry);
      }
    }

    // Report all unused scope entries
    for (ScopeEntry entry : scopeEntries) {
      if (!entry.getUsages().isEmpty()) {
        continue;
      }

      AstNode entryNode = entry.getNode();
      String name = entryNode.getTokenValue();
      String message = String.format(MESSAGE, name);
      addIssue(message, entryNode);
    }
  }


}
