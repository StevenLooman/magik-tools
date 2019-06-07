package org.stevenlooman.sw.magik.checks;

import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.analysis.scope.Scope;
import org.stevenlooman.sw.magik.analysis.scope.ScopeBuilderVisitor;
import org.stevenlooman.sw.magik.analysis.scope.ScopeEntry;
import org.stevenlooman.sw.magik.api.MagikGrammar;
import org.stevenlooman.sw.magik.api.MagikPunctuator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = UnusedVariableCheck.CHECK_KEY)
public class UnusedVariableCheck extends MagikCheck {

  private static final String MESSAGE = "Remove the unused local variable \"%s\".";
  public static final String CHECK_KEY = "UnusedVariable";
  private boolean checkParameters;

  ScopeBuilderVisitor scopeBuilder;
  Set<AstNode> usedIdentifiers = new HashSet<>();

  public UnusedVariableCheck() {
    this.checkParameters = false;
  }

  public UnusedVariableCheck(boolean checkParameters) {
    this.checkParameters = checkParameters;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.MAGIK,
        MagikGrammar.ATOM);
  }

  @Override
  public void visitNode(AstNode node) {
    // ensure part of global scope
    if (node.getType() == MagikGrammar.MAGIK) {
      // construct scope
      scopeBuilder = new ScopeBuilderVisitor();
      scopeBuilder.scanNode(node);
    } else if (node.getType() == MagikGrammar.ATOM) {
      // save used identifiers
      AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
      if (identifierNode == null) {
        return;
      }

      if (isLhsOfAssignment(identifierNode) && !isGlobalOrDynamic(identifierNode)) {
        return;
      }

      usedIdentifiers.add(identifierNode);
    }
  }

  private boolean isGlobalOrDynamic(AstNode identifierNode) {
    String identifier = identifierNode.getTokenValue();

    Scope scope = scopeBuilder.getScopeForNode(identifierNode);
    ScopeEntry scopeEntry = scope.getScopeEntry(identifier);

    return scopeEntry.getType() == ScopeEntry.Type.GLOBAL
           || scopeEntry.getType() == ScopeEntry.Type.DYNAMIC;
  }

  private boolean isLhsOfAssignment(AstNode identifierNode) {
    AstNode atomNode = identifierNode.getParent();
    if (atomNode.getType() != MagikGrammar.ATOM) {
      return false;
    }

    AstNode parent = atomNode.getParent();
    return (parent.getType() == MagikGrammar.ASSIGNMENT_EXPRESSION
            || parent.getType() == MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION)
           && parent.getFirstChild() == atomNode;
  }

  private boolean isAssignedToDirectly(AstNode identifierNode) {
    AstNode variableDeclarationNode = identifierNode.getParent();
    if (variableDeclarationNode.getType() != MagikGrammar.VARIABLE_DECLARATION) {
      return false;
    }

    if (variableDeclarationNode.getFirstChild() != identifierNode) {
      return false;
    }

    List<AstNode> chrevronChildren = variableDeclarationNode.getChildren(
        MagikPunctuator.CHEVRON,
        MagikPunctuator.BOOT_CHEVRON);
    return !chrevronChildren.isEmpty();
  }

  private boolean isPartOfMultiVariableDeclaration(AstNode identifierNode) {
    AstNode identifiersNode = identifierNode.getParent();
    if (identifiersNode == null
        || identifiersNode.getType() != MagikGrammar.IDENTIFIERS_WITH_GATHER) {
      return false;
    }
    AstNode multiVarDeclNode = identifiersNode.getParent();
    if (multiVarDeclNode == null
        || multiVarDeclNode.getType() != MagikGrammar.MULTI_VARIABLE_DECLARATION) {
      return false;
    }

    return true;
  }

  private boolean anyNextSiblingUsed(AstNode identifierNode) {
    AstNode sibling = identifierNode.getNextSibling();
    while (sibling != null) {
      if (sibling.getType() != MagikGrammar.IDENTIFIER) {
        sibling = sibling.getNextSibling();
        continue;
      }

      for (AstNode usedIdentifier: usedIdentifiers) {
        Scope scope = scopeBuilder.getScopeForNode(usedIdentifier);
        String identifierName = usedIdentifier.getTokenValue();
        ScopeEntry scopeEntry = scope.getScopeEntry(identifierName);
        if (scopeEntry != null) {
          return true;
        }

      }

      sibling = sibling.getNextSibling();
    }

    return false;
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.getType() == MagikGrammar.ATOM) {
      return;
    }

    // Gather all defined variables
    Set<AstNode> declaredIdentifiers = new HashSet<>();
    Scope globalScope = scopeBuilder.getScope();
    for (Scope scope: globalScope.getSelfAndDescendantScopes()) {
      for (ScopeEntry scopeEntry: scope.getScopeEntries()) {
        AstNode scopeEntryNode = scopeEntry.getNode();

        // But not globals/dynamics which is assigned to directly
        if ((scopeEntry.getType() == ScopeEntry.Type.GLOBAL
             || scopeEntry.getType() == ScopeEntry.Type.DYNAMIC)
            && isAssignedToDirectly(scopeEntryNode)) {
          continue;
        }

        if (!checkParameters && scopeEntry.getType() == ScopeEntry.Type.PARAMETER) {
          continue;
        }

        declaredIdentifiers.add(scopeEntryNode);
      }
    }

    // Import is also a use
    for (Scope scope: globalScope.getSelfAndDescendantScopes()) {
      for (ScopeEntry scopeEntry : scope.getScopeEntries()) {
        if (scopeEntry.getType() == ScopeEntry.Type.IMPORT) {
          ScopeEntry parentEntry = scopeEntry.getParentEntry();
          if (parentEntry != null) {
            AstNode parentEntryNode = parentEntry.getNode();
            declaredIdentifiers.remove(parentEntryNode);
          }
        }
      }
    }

    // Remove all defined variables when they are used
    for (AstNode identifierNode: usedIdentifiers) {
      Scope scope = scopeBuilder.getScopeForNode(identifierNode);
      String identifierName = identifierNode.getTokenValue();
      ScopeEntry scopeEntry = scope.getScopeEntry(identifierName);
      if (scopeEntry != null) {
        AstNode scopeEntryNode = scopeEntry.getNode();
        declaredIdentifiers.remove(scopeEntryNode);
      }
    }

    // Remove all defined variables which are:
    // - part of a MULTI_VARIABLE_DECLARATION
    // - the later identifiers of it are used
    // - but this one isn't
    for (AstNode declaredIdentifier: Sets.newHashSet(declaredIdentifiers)) {
      if (isPartOfMultiVariableDeclaration(declaredIdentifier)
          && anyNextSiblingUsed(declaredIdentifier)) {
        declaredIdentifiers.remove(declaredIdentifier);
      }
    }

    // Report all unused declarations
    for (AstNode identifierNode: declaredIdentifiers) {
      String name = identifierNode.getTokenValue();
      String message = String.format(MESSAGE, name);
      addIssue(message, identifierNode);
    }

    scopeBuilder = null;
  }

}
