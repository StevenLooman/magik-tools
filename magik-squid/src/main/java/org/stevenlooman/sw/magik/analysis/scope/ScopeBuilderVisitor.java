package org.stevenlooman.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.stevenlooman.sw.magik.MagikVisitor;
import org.stevenlooman.sw.magik.api.MagikGrammar;

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
    if (nodeType == MagikGrammar.BODY) {
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

  private void visitNodeBody(AstNode node) {
    // push new scope
    AstNode parentNode = node.getParent();
    if (parentNode.getType() == MagikGrammar.METHOD_DEFINITION
        || parentNode.getType() == MagikGrammar.PROC_DEFINITION) {
      scope = new ProcedureScope(scope, node);

      // add parameters to scope
      AstNode parametersNode = parentNode.getFirstChild(MagikGrammar.PARAMETERS);
      if (parametersNode != null) {
        List<AstNode> identifierNodes = parametersNode.getDescendants(MagikGrammar.IDENTIFIER);
        for (AstNode identifierNode : identifierNodes) {
          AstNode parameterNode = identifierNode.getParent();
          String identifier = identifierNode.getTokenValue();
          scope.addDeclaration(ScopeEntry.Type.PARAMETER, identifier, parameterNode, null);
        }
      }

      // add assignment parameter to scope
      AstNode assignmentParameterNode = parentNode.getFirstChild(MagikGrammar.ASSIGNMENT_PARAMETER);
      if (assignmentParameterNode != null) {
        AstNode parameterNode = assignmentParameterNode.getFirstChild(MagikGrammar.PARAMETER);
        String identifier = parameterNode.getTokenValue();
        scope.addDeclaration(ScopeEntry.Type.PARAMETER, identifier, parameterNode, null);
      }

      scopeIndex.put(parentNode, scope);  // handy
    } else if (parentNode.getType() == MagikGrammar.TRY_BLOCK) {
      // XXX TODO: _try _with cond ???
      scope = new BodyScope(scope, node);

      AstNode identifiersNode = parentNode.getFirstChild(MagikGrammar.IDENTIFIERS);
      List<AstNode> identifierNodes = identifiersNode.getChildren(MagikGrammar.IDENTIFIER);
      for (AstNode identifierNode: identifierNodes) {
        String identifier = identifierNode.getTokenValue();
        scope.addDeclaration(ScopeEntry.Type.PARAMETER, identifier, identifierNode, null);
      }

      scopeIndex.put(parentNode, scope);
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

    return scope; // get global scope
  }

}
