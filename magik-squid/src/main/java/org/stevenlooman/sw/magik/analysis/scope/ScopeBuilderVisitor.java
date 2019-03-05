package org.stevenlooman.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.stevenlooman.sw.magik.MagikVisitor;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScopeBuilderVisitor extends MagikVisitor {

  private Scope scope;
  private Map<AstNode, Scope> scopeIndex = new HashMap<>();
  private List<AstNode> tempStorage;

  public ScopeBuilderVisitor() {
    scope = new GlobalScope();
  }

  public Scope getScope() {
    return scope;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.METHOD_DEFINITION,
        MagikGrammar.PROC_DEFINITION,
        MagikGrammar.BODY,
        MagikGrammar.ASSIGNMENT_EXPRESSION,
        MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION,
        MagikGrammar.VARIABLE_DECLARATION,
        MagikGrammar.MULTI_VARIABLE_DECLARATION
    );
  }

  @Override
  public void visitNode(AstNode node) {
    // dispatch
    AstNodeType nodeType = node.getType();
    if (nodeType == MagikGrammar.METHOD_DEFINITION || nodeType == MagikGrammar.PROC_DEFINITION) {
      visitNodeProcDefinition(node);
    } else if (nodeType == MagikGrammar.BODY) {
      visitNodeBody(node);
    } else if (nodeType == MagikGrammar.ASSIGNMENT_EXPRESSION
        || nodeType == MagikGrammar.AUGMENTED_ASSIGNMENT_EXPRESSION) {
      visitNodeAssignmentExpression(node);
    } else if (nodeType == MagikGrammar.VARIABLE_DECLARATION) {
      visitNodeVariableDeclaration(node);
    } else if (nodeType == MagikGrammar.MULTI_VARIABLE_DECLARATION) {
      visitNodeMultiVariableDeclaration(node);
    }
  }

  private void visitNodeProcDefinition(AstNode node) {
    tempStorage = new ArrayList<>();

    AstNode parametersNode = node.getFirstChild(MagikGrammar.PARAMETERS);
    if (parametersNode == null) {
      return;
    }

    List<AstNode> parameterNodes = parametersNode.getChildren(MagikGrammar.PARAMETER);
    for (AstNode parameterNode: parameterNodes) {
      AstNode identifierNode = parameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
      tempStorage.add(identifierNode);
    }
  }

  private void visitNodeBody(AstNode node) {
    // push new scope
    if (node.getParent().getType() == MagikGrammar.METHOD_DEFINITION
        || node.getParent().getType() == MagikGrammar.PROC_DEFINITION) {
      scope = new ProcedureScope(scope, node);

      for (AstNode tempNode: tempStorage) {
        String identifier = tempNode.getTokenValue();
        scope.addDeclaration(ScopeEntry.Type.PARAMETER, identifier, tempNode, null);
      }
    } else {
      scope = new BodyScope(scope, node);
    }

    scopeIndex.put(node, scope);
  }

  private void visitNodeVariableDeclaration(AstNode node) {
    AstNode variableDeclarationStatement = node.getParent();
    String type = variableDeclarationStatement.getTokenValue().toUpperCase().substring(1);
    ScopeEntry.Type scopeEntryType = ScopeEntry.Type.valueOf(type);

    String identifier = node.getTokenValue();
    if (scope.getScopeEntry(identifier) != null) {
      // don't overwrite entries
      return;
    }

    ScopeEntry parentEntry = null;
    if (scopeEntryType == ScopeEntry.Type.IMPORT) {
      parentEntry = scope.getParentScope().getScopeEntry(identifier);
    }

    AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
    scope.addDeclaration(scopeEntryType, identifier, identifierNode, parentEntry);
  }

  private void visitNodeMultiVariableDeclaration(AstNode node) {
    AstNode variableDeclarationStatement = node.getParent();
    String type = variableDeclarationStatement.getTokenValue().toUpperCase().substring(1);
    ScopeEntry.Type scopeEntryType = ScopeEntry.Type.valueOf(type);

    AstNode identifiersNode = node.getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER);
    List<AstNode> identifierNodes = identifiersNode.getChildren(MagikGrammar.IDENTIFIER);
    for (AstNode identifierNode: identifierNodes) {
      String identifier = identifierNode.getTokenValue();
      if (scope.getScopeEntry(identifier) != null) {
        // don't overwrite entries
        continue;
      }

      scope.addDeclaration(scopeEntryType, identifier, identifierNode, null);
    }
  }

  private void visitNodeAssignmentExpression(AstNode node) {
    AstNode atomNode = node.getFirstChild(MagikGrammar.ATOM);
    if (atomNode == null) {
      return;
    }

    AstNode identifierNode = atomNode.getFirstChild(MagikGrammar.IDENTIFIER);
    if (identifierNode == null) {
      return;
    }

    String identifier = identifierNode.getTokenValue();
    if (scope.getScopeEntry(identifier) != null) {
      // don't overwrite entries
      return;
    }

    scope.addDeclaration(ScopeEntry.Type.DEFINITION, identifier, identifierNode, null);
  }

  @Override
  public void leaveNode(AstNode node) {
    // dispatch
    if (node.getType() == MagikGrammar.BODY) {
      leaveNodeBody(node);
    }
  }

  private void leaveNodeBody(AstNode node) {
    // pop current scope
    scope = scope.getParentScope();
  }

  public Scope scopeForNode(AstNode node) {
    // find scope for this node
    AstNode currentNode = node;
    while (currentNode != null) {
      if (scopeIndex.containsKey(currentNode)) {
        return scopeIndex.get(currentNode);
      }

      currentNode = currentNode.getParent();
    }

    return scope; // get global scope
  }

}
