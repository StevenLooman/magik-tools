package org.stevenlooman.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;

import org.stevenlooman.sw.magik.MagikVisitor;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScopeBuilderVisitor extends MagikVisitor {

  /**
   * Global scope.
   */
  private GlobalScope globalScope;

  /**
   * Current scope.
   */
  private Scope scope;

  /**
   * Scope index for quick searching.
   */
  private Map<AstNode, Scope> scopeIndex = new HashMap<>();

  /**
   * Constructor.
   */
  public ScopeBuilderVisitor() {
  }

  /**
   * Get the {{GlobalScope}}.
   * @return Global scope
   */
  public GlobalScope getGlobalScope() {
    return globalScope;
  }

  @Override
  protected void walkPreMagik(AstNode node) {
    scope = globalScope = new GlobalScope(scopeIndex, node);
  }

  @Override
  protected void walkPreBody(AstNode node) {
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

      // add indexer parameters
      AstNode indexerParametersNode = parentNode.getFirstChild(MagikGrammar.INDEXER_PARAMETERS);
      if (indexerParametersNode != null) {
        List<AstNode> identifierNodes =
            indexerParametersNode.getDescendants(MagikGrammar.IDENTIFIER);
        for (AstNode identifierNode : identifierNodes) {
          AstNode parameterNode = identifierNode.getParent();
          String identifier = identifierNode.getTokenValue();
          scope.addDeclaration(ScopeEntry.Type.PARAMETER, identifier, parameterNode, null);
        }
      }

      // add assignment parameter to scope
      AstNode assignmentParameterNode = parentNode.getFirstChild(MagikGrammar.ASSIGNMENT_PARAMETER);
      if (assignmentParameterNode != null) {
        AstNode identifierNode = assignmentParameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
        String identifier = identifierNode.getTokenValue();
        scope.addDeclaration(ScopeEntry.Type.PARAMETER, identifier, identifierNode, null);
      }
    } else if (parentNode.getType() == MagikGrammar.WHEN) {
      scope = new BodyScope(scope, node);

      // add _with items to scope
      AstNode tryNode = parentNode.getParent();
      AstNode identifiersNode = tryNode.getFirstChild(MagikGrammar.IDENTIFIERS);
      if (identifiersNode != null) {
        List<AstNode> identifierNodes = identifiersNode.getChildren(MagikGrammar.IDENTIFIER);
        for (AstNode identifierNode : identifierNodes) {
          String identifier = identifierNode.getTokenValue();
          scope.addDeclaration(ScopeEntry.Type.LOCAL, identifier, identifierNode, null);
        }
      }
    } else if (parentNode.getType() == MagikGrammar.LOOP) {
      scope = new BodyScope(scope, node);

      // add for-items to scope
      AstNode loopNode = node.getParent();
      AstNode overNode = loopNode.getParent();
      if (overNode.getType() == MagikGrammar.OVER) {
        AstNode forNode = overNode.getParent();
        if (forNode.getType() == MagikGrammar.FOR) {
          AstNode forIdentifiersNode = forNode.getFirstChild(MagikGrammar.FOR_IDENTIFIERS);
          AstNode identifiersNode = forIdentifiersNode.getFirstChild(
              MagikGrammar.IDENTIFIERS_WITH_GATHER);
          List<AstNode> identifierNodes = identifiersNode.getChildren(MagikGrammar.IDENTIFIER);
          for (AstNode identifierNode: identifierNodes) {
            String identifier = identifierNode.getTokenValue();
            scope.addDeclaration(ScopeEntry.Type.LOCAL, identifier, identifierNode, null);
          }
        }
      }
    } else {
      // regular scope
      scope = new BodyScope(scope, node);
    }

    scopeIndex.put(node, scope);
  }

  @Override
  protected void walkPreVariableDefinitionStatement(AstNode node) {
    String type = node.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_MODIFIER)
        .getTokenValue().toUpperCase().substring(1);
    ScopeEntry.Type scopeEntryType = ScopeEntry.Type.valueOf(type);

    AstNode varDefMultiNode = node.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_MULTI);
    List<AstNode> identifierNodes;
    if (varDefMultiNode != null) {
      AstNode identifiersWithGatherNode =
          varDefMultiNode.getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER);
      identifierNodes = identifiersWithGatherNode.getChildren(MagikGrammar.IDENTIFIER);
    } else {
      identifierNodes = new ArrayList<>();
      for (AstNode varDefNode: node.getChildren(MagikGrammar.VARIABLE_DEFINITION)) {
        AstNode identifierNode = varDefNode.getFirstChild(MagikGrammar.IDENTIFIER);
        identifierNodes.add(identifierNode);
      }
    }

    for (AstNode identifierNode : identifierNodes) {
      String identifier = identifierNode.getTokenValue();
      if (scope.getScopeEntry(identifier) != null) {
        // don't overwrite entries
        continue;
      }

      ScopeEntry parentEntry = null;
      if (scopeEntryType == ScopeEntry.Type.IMPORT) {
        parentEntry = scope.getParentScope().getScopeEntry(identifier);
      }

      scope.addDeclaration(scopeEntryType, identifier, identifierNode, parentEntry);
      // parentEntry gets a usage via the constructor of the added declaration
    }
  }

  @Override
  protected void walkPreMultipleAssignmentStatement(AstNode node) {
    AstNode identifiersNode = node.getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER);
    List<AstNode> identifierNodes = identifiersNode.getChildren(MagikGrammar.IDENTIFIER);
    for (AstNode identifierNode: identifierNodes) {
      String identifier = identifierNode.getTokenValue();
      if (scope.getScopeEntry(identifier) != null) {
        // don't overwrite entries
        continue;
      }

      scope.addDeclaration(ScopeEntry.Type.DEFINITION, identifier, identifierNode, null);
    }
  }

  @Override
  protected void walkPreAssignmentExpression(AstNode node) {
    // get all atoms to the last <<
    Integer lastAssignmentTokenIndex = node.getChildren().stream()
        .filter(childNode -> childNode.getTokenValue().equals("<<")
                            || childNode.getTokenValue().equals("^<<"))
        .map(childNode -> node.getChildren().indexOf(childNode))
        .max(Comparator.naturalOrder()).get();
    List<AstNode> childNodes = node.getChildren().subList(0, lastAssignmentTokenIndex);
    for (AstNode childNode : childNodes) {
      if (childNode.getType() != MagikGrammar.ATOM) {
        continue;
      }

      AstNode identifierNode = childNode.getFirstChild(MagikGrammar.IDENTIFIER);
      if (identifierNode == null) {
        return;
      }

      String identifier = identifierNode.getTokenValue();
      if (scope.getScopeEntry(identifier) != null) {
        // don't overwrite entries
        return;
      }

      // add as definition
      scope.addDeclaration(ScopeEntry.Type.DEFINITION, identifier, identifierNode, null);
    }
  }

  @Override
  protected void walkPreAtom(AstNode node) {
    AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
    if (identifierNode == null) {
      return;
    }

    String identifier = identifierNode.getTokenValue();
    ScopeEntry existingScopeEntry = scope.getScopeEntry(identifier);
    if (existingScopeEntry != null) {
      if (existingScopeEntry.getNode() != identifierNode) {
        existingScopeEntry.addUsage(node);
      }

      // don't overwrite entries
      return;
    }

    // add as global, and use directly
    ScopeEntry entry = scope.addDeclaration(ScopeEntry.Type.GLOBAL, identifier, node, null);
    entry.addUsage(node);
  }

  @Override
  protected void walkPostBody(AstNode node) {
    // pop current scope
    scope = scope.getParentScope();
  }

}
